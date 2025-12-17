package clinixpay.license.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@Document(collection = "users")
public class User {

    @Id
    private String id;

    private String fullName;

    @Indexed(unique = true)
    private String email;

    private String mobileNumber;

    // --- Key Generation Fields ---
    @Indexed(unique = true)
    private String licensekey;           // Stores the HASHED key for verification

    // *** NEW FIELD: Stores the plain key temporarily until payment is verified and email is sent. ***
    // Must be set to null immediately after successful key delivery.
    private String tempPlainLoginKey;
    // ***************************

    private KeyStatus keyStatus = KeyStatus.PENDING_PAYMENT; // Default is PENDING_PAYMENT
    private LocalDateTime keyGenerationTime;
    private LocalDateTime keyExpiryTime;

    // --- Payment Fields ---
    private String selectedPlan;
    private Long planAmountPaise; // Amount in the smallest currency unit (paise for INR)
    private String razorpayOrderId; // ID received after creating the order
    private String razorpayPaymentId; // ID received after successful payment

    private LocalDateTime registrationTime = LocalDateTime.now();
}