package Clinixpay.ClinicPaykeyGeneration.model;



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

    @Indexed(unique = true) // Ensures no two users share the same email
    private String email;

    private String mobileNumber;

    // --- Key Generation Fields ---
    // Indexed for fast lookup to check key uniqueness
    @Indexed(unique = true)
    private String loginKey;           // Stores the secure, hashed version of the 12-digit key

    private KeyStatus keyStatus = KeyStatus.ACTIVE;
    private LocalDateTime keyGenerationTime = LocalDateTime.now();
    private LocalDateTime keyExpiryTime;

    private LocalDateTime registrationTime = LocalDateTime.now();
}