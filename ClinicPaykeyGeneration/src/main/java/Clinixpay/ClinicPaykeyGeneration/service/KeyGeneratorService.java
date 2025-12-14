package Clinixpay.ClinicPaykeyGeneration.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class KeyGeneratorService {

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    /**
     * Generates a unique 12-digit key.
     */
    public String generateUnique12DigitKey() {
        // NOTE: Ensure your actual production code generates a truly unique key,
        // perhaps by checking the database or using a UUID/secure random generation.
        // This example uses a simple approach for demonstration.

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
}