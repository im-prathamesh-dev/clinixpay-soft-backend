package clinixpay.license.controller;

import clinixpay.license.dto.LicensePurchaseRequest;
import clinixpay.license.dto.LicenseValidationRequest;
import clinixpay.license.dto.PaymentResponse;
import clinixpay.license.dto.PaymentVerificationRequest;
import clinixpay.license.model.KeyStatus;
import clinixpay.license.model.User;
import clinixpay.license.service.LicenseService;
import clinixpay.license.service.PaymentService;
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
@RequestMapping("/api/license")
public class LicenseController {

    @Autowired
    private LicenseService licenseService;

    @Autowired
    private PaymentService paymentService;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    /**
     * 1️⃣ Purchase License (Free or Paid)
     * POST /api/license/purchase
     */
    @PostMapping("/purchase")
    public ResponseEntity<?> purchaseLicense(
            @Valid @RequestBody LicensePurchaseRequest request
    ) {
        try {
            User user = licenseService.initiateLicensePurchase(request);

            // Free plan → instantly active
            if (user.getKeyStatus() == KeyStatus.ACTIVE) {
                return ResponseEntity.ok(Map.of(
                        "message", "Free plan activated. License key sent to email.",
                        "status", "ACTIVE"
                ));
            }

            // Paid plan → Razorpay order
            PaymentResponse response = new PaymentResponse();
            response.setUserId(user.getId());
            response.setOrderId(user.getRazorpayOrderId());
            response.setAmountInPaise(user.getPlanAmountPaise());
            response.setRazorpayKeyId(razorpayKeyId);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        } catch (RazorpayException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Payment initiation failed"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "License purchase failed"));
        }
    }

    /**
     * 2️⃣ Verify Razorpay Payment
     * POST /api/license/verify-payment
     */
    @PostMapping("/verify-payment")
    public ResponseEntity<?> verifyPayment(
            @RequestBody PaymentVerificationRequest request
    ) {

        boolean isValid = paymentService.verifyPaymentSignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );

        if (!isValid) {
            return ResponseEntity.badRequest()
                    .body("Invalid Razorpay signature");
        }

        try {
            licenseService.completeLicenseActivation(
                    request.getUserId(),
                    request.getRazorpayPaymentId()
            );
            return ResponseEntity.ok("Payment verified. License activated.");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Payment verified but license activation failed");
        }
    }

    /**
     * 3️⃣ Validate License (Used by Billing Software)
     * POST /api/license/validate
     */
    @PostMapping("/validate")
    public ResponseEntity<?> validateLicense(
            @Valid @RequestBody LicenseValidationRequest request
    ) {
        try {
            User user = licenseService.validateUserLicense(
                    request.getEmail(),
                    request.getLicenseKey()
            );

            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "active", user.getKeyStatus() == KeyStatus.ACTIVE,
                    "plan", user.getSelectedPlan(),
                    "email", user.getEmail()
            ));

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("valid", false, "error", e.getMessage()));
        }
    }

    /**
     * Global DTO validation handler
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> handleValidationExceptions(
            MethodArgumentNotValidException ex
    ) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            errors.put(
                    ((FieldError) error).getField(),
                    error.getDefaultMessage()
            );
        });
        return errors;
    }
}
