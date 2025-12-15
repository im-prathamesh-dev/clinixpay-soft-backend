package Clinixpay.ClinicPaykeyGeneration.controller;

import Clinixpay.ClinicPaykeyGeneration.dto.LoginRequest; // <--- NEW IMPORT
import Clinixpay.ClinicPaykeyGeneration.dto.PaymentResponse;
import Clinixpay.ClinicPaykeyGeneration.dto.PaymentVerificationRequest;
import Clinixpay.ClinicPaykeyGeneration.dto.RegistrationRequest;
import Clinixpay.ClinicPaykeyGeneration.model.KeyStatus;
import Clinixpay.ClinicPaykeyGeneration.model.User;
import Clinixpay.ClinicPaykeyGeneration.service.PaymentService;
import Clinixpay.ClinicPaykeyGeneration.service.RegistrationService;
import com.razorpay.RazorpayException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/register")
public class RegistrationController {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private PaymentService paymentService;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    /**
     * 1. Endpoint to initiate registration for both Free (Plan 0) and Paid plans (1, 2, 3).
     * - URL: POST http://localhost:8081/api/register/initiate-payment
     */
    @PostMapping("/initiate-payment")
    public ResponseEntity<?> initiatePayment(@Valid @RequestBody RegistrationRequest request) {

        try {
            User newUser = registrationService.initiateRegistrationAndPayment(request);

            if (newUser.getKeyStatus() == KeyStatus.ACTIVE) {
                // Scenario 1: Free Plan (Plan 0) - Registration is completed immediately.
                String message = String.format(
                        "SUCCESS: Free Plan (%s) activated. A unique key has been sent to %s.",
                        newUser.getSelectedPlan(),
                        newUser.getEmail()
                );
                // Return structured JSON response
                return ResponseEntity.ok(Map.of("message", message, "status", "ACTIVATED"));

            } else {
                // Scenario 2: Paid Plans (Plan 1, 2, 3) - Razorpay Order created, payment pending.

                // Respond with the details the frontend needs to open the Razorpay popup
                PaymentResponse response = new PaymentResponse();
                response.setUserId(newUser.getId());
                response.setOrderId(newUser.getRazorpayOrderId());
                response.setAmountInPaise(newUser.getPlanAmountPaise());
                response.setRazorpayKeyId(razorpayKeyId);

                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            }

        } catch (IllegalStateException e) {
            // Handles if the user email already exists
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (RazorpayException e) {
            // Razorpay Order creation failed
            System.err.println("Razorpay Order Creation Failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Payment initiation failed: " + e.getMessage()));
        } catch (Exception e) {
            // Catches any remaining unhandled exception
            System.err.println("Fatal Registration/Payment error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Registration failed due to a critical server error."));
        }
    }

    /**
     * 2. Endpoint for the client/webhook to verify the payment and complete registration
     * for PAID plans (1, 2, 3).
     * - URL: POST http://localhost:8081/api/register/verify-payment
     */
    @PostMapping("/verify-payment")
    public ResponseEntity<String> verifyPayment(@RequestBody PaymentVerificationRequest request) {

        // CRITICAL: Verify the signature first to ensure the request is authentic
        boolean isValidSignature = paymentService.verifyPaymentSignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );

        if (isValidSignature) {
            try {
                // Complete the registration process (Plan 1, 2, or 3 logic)
                registrationService.completeRegistration(request.getUserId(), request.getRazorpayPaymentId());

                return ResponseEntity.ok("Payment verified and registration completed. Key email sent successfully.");

            } catch (IllegalStateException e) {
                // User not found or not in PENDING_PAYMENT status
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Verification failed: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("Fatal Payment Verification error: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Payment verified but registration completion failed due to server error: " + e.getMessage());
            }
        } else {
            // Log the fraudulent attempt
            System.err.println("ALERT: Invalid Razorpay Signature detected for Order ID: " + request.getRazorpayOrderId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payment verification failed: Invalid Signature.");
        }
    }

    /**
     * 3. NEW ENDPOINT: Validates the user's provided email and license key to grant access.
     * - URL: POST http://localhost:8081/api/register/validate-license
     */
    @PostMapping("/validate-license")
    public ResponseEntity<?> validateLicense(@Valid @RequestBody LoginRequest request) {
        try {
            User user = registrationService.validateUserLicense(request.getEmail(), request.getLicenseKey());

            // Key Validation Successful
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Key Detected. Access Granted after successful registration.");
            response.put("keyStatus", user.getKeyStatus());
            response.put("email", user.getEmail());
            response.put("plan", user.getSelectedPlan());

            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            // Validation failed (User not found, key mismatch, or inactive status)
            // Use 401 Unauthorized since the credentials failed the check
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // General server error
            System.err.println("Fatal License Validation error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An internal error occurred during validation."));
        }
    }

    /**
     * Custom handler to catch MethodArgumentNotValidException thrown when @Valid fails.
     * Returns a clear JSON map of field errors to the client with a 400 status.
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return errors;
    }
}