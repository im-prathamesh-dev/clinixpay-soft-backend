package clinixpay.license.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class LicensePurchaseRequest {

    // Common regex for email validation (RFC 5322 standard approximation)
    private static final String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$";

    @NotBlank(message = "Full name is required.")
    private String fullName;

    /**
     * Replaced @Email annotation with a custom @Pattern regex.
     * This regex is generally stricter than the default @Email implementation.
     */
    @NotBlank(message = "Email is required.")
    @Pattern(regexp = EMAIL_REGEX, message = "Invalid email format. Please check for special characters or domain structure.")
    private String email;

    @NotBlank(message = "Mobile number is required.")
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits.")
    private String mobileNumber;

    @NotNull(message = "Plan ID is required.")
    @Min(value = 0, message = "Invalid plan ID selected.")
    private Integer planId;
}