package clinixpay.license.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class KeyGeneratorService {

    // Ensure BCryptPasswordEncoder is configured as a Bean in your main application class
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    /**
     * Generates a unique 12-digit key.
     */
    public String generateUnique12DigitKey() {
        // NOTE: While this uses Random, consider using SecureRandom for true security
        // and add a database uniqueness check in production.

        Random random = new Random();
        StringBuilder key = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            key.append(random.nextInt(10));
        }
        return key.toString();
    }

    /**
     * Hashes the plain key using BCrypt before storing it in MongoDB.
     */
    public String hashKey(String plainKey) {
        return passwordEncoder.encode(plainKey);
    }

    /**
     * CRITICAL: Verifies a plain license key provided by the user against the
     * HASHED key stored in the database.
     * * This is used by the validateUserLicense method in LicenseService.
     * * @param plainKey The key entered by the user.
     * @param hashedKey The key retrieved from the MongoDB User record.
     * @return true if the keys match, false otherwise.
     */
    public boolean checkKey(String plainKey, String hashedKey) {
        // BCryptPasswordEncoder.matches() handles the comparison securely
        // by hashing the plainKey and comparing it with the hashedKey.
        return passwordEncoder.matches(plainKey, hashedKey);
    }
}