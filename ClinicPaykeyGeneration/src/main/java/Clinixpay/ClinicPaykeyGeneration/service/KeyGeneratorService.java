package Clinixpay.ClinicPaykeyGeneration.service;

import Clinixpay.ClinicPaykeyGeneration.repository.UserRepository;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class KeyGeneratorService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Generates a random, unique 12-digit numeric key.
     * Checks for uniqueness using the UserRepository method.
     * @return A unique plain text 12-digit numeric key.
     */
    public String generateUnique12DigitKey() {
        String plainKey;
        // The loop continues until a key is generated that is not found in the database.
        do {
            // Generate a random 12-character numeric string
            plainKey = RandomStringUtils.randomNumeric(12);

        } while (userRepository.findByLoginKey(plainKey).isPresent());

        return plainKey;
    }

    /**
     * Returns the key unchanged (no hashing).
     */
    public String identityKey(String plainKey) {
        return plainKey;
    }
}