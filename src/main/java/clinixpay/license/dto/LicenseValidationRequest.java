package clinixpay.license.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class LicenseValidationRequest {

    // Using a common strict email regex for validation
    private static final String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$";

    @NotBlank(message = "Email is required.")
    @Pattern(regexp = EMAIL_REGEX, message = "Invalid email format.")
    private String email;

    @NotBlank(message = "License key is required.")
    private String licenseKey; // The key the user enters from their email
}