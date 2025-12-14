package Clinixpay.ClinicPaykeyGeneration.controller;

import Clinixpay.ClinicPaykeyGeneration.dto.RegistrationRequest;
import Clinixpay.ClinicPaykeyGeneration.model.User;
import Clinixpay.ClinicPaykeyGeneration.service.RegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/register")
public class RegistrationController {

    @Autowired
    private RegistrationService registrationService;

    /**
     * Endpoint to register a user, generate a unique 12-digit key, and send the email.
     * * URL: POST http://localhost:8081/api/register/user
     */
    @PostMapping("/user")
    public ResponseEntity<String> registerUser(@Valid @RequestBody RegistrationRequest request) {

        try {
            User savedUser = registrationService.registerUser(request);

            // Check if the user was saved successfully
            if (savedUser.getId() != null) {
                // Since the email failure is now caught in the service, we assume success here.
                String responseMessage = String.format(
                        "SUCCESS: Registration complete. A unique 12-digit key has been sent to %s. (Check server logs for email status)",
                        request.getEmail()
                );
                return ResponseEntity.ok(responseMessage);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Registration failed: Database save failed unexpectedly.");
            }

        } catch (IllegalStateException e) {
            // Handles if the user email already exists (e.g., "User with email ... is already registered.")
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            // This catches any remaining unhandled exception (e.g., database connection down)
            System.err.println("Fatal Registration error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Registration failed due to a critical server error: " + e.getMessage());
        }
    }
}