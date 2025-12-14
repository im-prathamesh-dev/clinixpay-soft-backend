package Clinixpay.ClinicPaykeyGeneration.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegistrationRequest {

    @NotBlank(message = "Full Name is required")
    @Size(min = 3, message = "Full Name must be at least 3 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits")
    private String mobileNumber;

    @NotNull(message = "Plan ID is required")
    // --- UPDATED VALIDATION TO ALLOW PLAN 0 (FREE PLAN) ---
    @Min(value = 0, message = "Plan ID must be 0 (Free), 1, 2, or 3")
    private int planId;
}