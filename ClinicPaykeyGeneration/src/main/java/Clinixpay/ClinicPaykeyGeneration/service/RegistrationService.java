package Clinixpay.ClinicPaykeyGeneration.service;

import Clinixpay.ClinicPaykeyGeneration.dto.RegistrationRequest;
import Clinixpay.ClinicPaykeyGeneration.model.KeyStatus;
import Clinixpay.ClinicPaykeyGeneration.model.User;
import Clinixpay.ClinicPaykeyGeneration.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class RegistrationService {

    @Autowired private UserRepository userRepository;
    @Autowired private KeyGeneratorService keyGeneratorService;
    @Autowired private EmailService emailService;

    @Value("${key.validity.days}")
    private int keyValidityDays;

    /**
     * Registers a new user, generates a unique 12-digit key, saves the user, and attempts to send the key via email.
     * The user is saved to the database regardless of email failure.
     * @param request The user data from the API request.
     * @return The newly created User entity.
     * @throws IllegalStateException if the user's email is already registered.
     */
    public User registerUser(RegistrationRequest request) {

        // 1. Check for existing user (prevents registration with duplicate email)
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalStateException("User with email " + request.getEmail() + " is already registered.");
        }

        // 2. Generate Unique Key
        String plainLoginKey = keyGeneratorService.generateUnique12DigitKey();

        // 3. Build and Save User Entity
        User newUser = new User();
        newUser.setFullName(request.getFullName());
        newUser.setEmail(request.getEmail());
        newUser.setMobileNumber(request.getMobileNumber());
        newUser.setLoginKey(plainLoginKey); // Storing the PLAIN KEY
        newUser.setKeyStatus(KeyStatus.ACTIVE);
        newUser.setKeyGenerationTime(LocalDateTime.now());
        newUser.setKeyExpiryTime(LocalDateTime.now().plusDays(keyValidityDays));

        // *** Data Save: This should succeed and persist the user ***
        User savedUser = userRepository.save(newUser);

        // 4. Email Key (Handle email failure gracefully)
        try {
            emailService.sendLoginKeyEmail(request.getEmail(), plainLoginKey);
        } catch (Exception e) {
            // Log the email failure, but DO NOT rethrow, so the database save is not rolled back.
            // This is how the database save is protected from email failure.
            System.err.println("ALERT: Email delivery failed for user " + request.getEmail() + ". Key was saved but not delivered. Error: " + e.getMessage());
            // You might want to implement a retry mechanism here in a production environment.
        }

        return savedUser;
    }
}