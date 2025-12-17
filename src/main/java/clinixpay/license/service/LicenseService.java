package clinixpay.license.service;

import clinixpay.license.dto.LicensePurchaseRequest;
import clinixpay.license.model.KeyStatus;
import clinixpay.license.model.User;
import clinixpay.license.repository.UserRepository;
import com.razorpay.RazorpayException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
public class LicenseService {

    @Autowired private UserRepository userRepository;
    @Autowired private KeyGeneratorService keyGeneratorService;
    @Autowired private PaymentService paymentService;
    @Autowired private EmailService emailService;


    /**
     * Handles both Free Plan (direct activation) and Paid Plan (Razorpay Order creation).
     * This is called by POST /api/register/initiate-payment
     */
    public User initiateLicensePurchase(LicensePurchaseRequest request) throws RazorpayException {

        // 1. Check for existing user
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalStateException("User with email " + request.getEmail() + " is already registered.");
        }

        // 2. Determine Plan Details
        Long planAmountPaise = paymentService.getPlanAmountPaise(request.getPlanId());
        String planName = paymentService.getPlanName(request.getPlanId());
        boolean isFreePlan = planAmountPaise == 0L;

        // 3. Generate and Hash Key
        String plainLicensekey = keyGeneratorService.generateUnique12DigitKey();
        String hashedLicensekey = keyGeneratorService.hashKey(plainLicensekey);

        User newUser = new User();
        newUser.setFullName(request.getFullName());
        newUser.setEmail(request.getEmail());
        newUser.setMobileNumber(request.getMobileNumber());
        newUser.setSelectedPlan(planName);
        newUser.setPlanAmountPaise(planAmountPaise);
        newUser.setLicensekey(hashedLicensekey); // Store the HASHED key

        // 4. Conditional logic for Free vs. Paid
        if (isFreePlan) {
            // FREE PLAN: Activate immediately and send email.

            Map.Entry<Long, ChronoUnit> validity = paymentService.getPlanValidityDuration(request.getPlanId());

            newUser.setKeyStatus(KeyStatus.ACTIVE);
            newUser.setKeyGenerationTime(LocalDateTime.now());
            newUser.setKeyExpiryTime(LocalDateTime.now().plus(validity.getKey(), validity.getValue()));

            User savedUser = userRepository.save(newUser);

            // Send the success email immediately for the Free Plan (key is available)
            try {
                emailService.sendPaymentSuccessEmail(
                        savedUser.getEmail(),
                        savedUser.getFullName(),
                        plainLicensekey, // SEND THE PLAIN KEY
                        savedUser.getSelectedPlan(),
                        savedUser.getPlanAmountPaise()
                );
            } catch (Exception e) {
                System.err.println("ALERT: Free Plan Email failed for user " + savedUser.getEmail() + ". Error: " + e.getMessage());
            }

            return savedUser;

        } else {
            // PAID PLAN: Set PENDING_PAYMENT and create Razorpay Order.
            newUser.setKeyStatus(KeyStatus.PENDING_PAYMENT);

            // *** CRITICAL UPDATE: Store PLAIN key temporarily for post-payment email ***
            newUser.setTempPlainLoginKey(plainLicensekey);

            User savedUser = userRepository.save(newUser);

            String razorpayOrderId = paymentService.createRazorpayOrder(planAmountPaise, savedUser.getId());
            savedUser.setRazorpayOrderId(razorpayOrderId);

            return userRepository.save(savedUser);
        }
    }


    /**
     * Finalizes user registration after successful payment verification.
     * This is called by POST /api/register/verify-payment
     */
    public User completeLicenseActivation(String userId, String paymentId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found with ID: " + userId));

        if (user.getKeyStatus() != KeyStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("User is not in PENDING_PAYMENT status.");
        }

        // 1. DETERMINE DYNAMIC EXPIRY for Paid Plans
        int planId = 0;
        try {
            String name = user.getSelectedPlan();
            String idPart = name.substring(name.lastIndexOf(" ") + 1);
            planId = Integer.parseInt(idPart);
        } catch (Exception e) {
            System.err.println("Warning: Could not parse plan ID from selectedPlan: " + user.getSelectedPlan());
        }

        Map.Entry<Long, ChronoUnit> validity = paymentService.getPlanValidityDuration(planId);
        LocalDateTime expiryTime = LocalDateTime.now().plus(validity.getKey(), validity.getValue());

        // --- Retrieve the plain key for the email BEFORE clearing it ---
        String plainLoginKey = user.getTempPlainLoginKey();

        // 2. Update User status, key validity, and payment details
        user.setKeyStatus(KeyStatus.ACTIVE);
        user.setKeyGenerationTime(LocalDateTime.now());
        user.setKeyExpiryTime(expiryTime);
        user.setRazorpayPaymentId(paymentId);

        // *** CRITICAL UPDATE: Clear the temporary key for security ***
        user.setTempPlainLoginKey(null);

        User completedUser = userRepository.save(user); // Save the ACTIVATED user

        // 3. Send the success email (now using the retrieved PLAIN key)
        if (plainLoginKey != null) {
            try {
                emailService.sendPaymentSuccessEmail(
                        completedUser.getEmail(),
                        completedUser.getFullName(),
                        plainLoginKey, // <--- SUCCESS: Sending the plain key!
                        completedUser.getSelectedPlan(),
                        completedUser.getPlanAmountPaise()
                );
            } catch (Exception e) {
                System.err.println("ALERT: Paid Plan Success Email delivery failed for user " + completedUser.getEmail() + ". Error: " + e.getMessage());
            }
        } else {
            System.err.println("CRITICAL ALERT: Plain Login Key missing for user " + completedUser.getEmail() + " after payment verification. Email NOT SENT.");
        }

        return completedUser;
    }


    /**
     * VALIDATION/LOGIN FUNCTIONALITY:
     * Validates user credentials (email and provided license key) against stored data.
     * @param email The user's email address.
     * @param plainLicenseKey The license key provided by the user (unhashed).
     * @return The User object if credentials are valid and key is ACTIVE.
     * @throws IllegalStateException if validation fails.
     */
    public User validateUserLicense(String email, String plainLicenseKey) {
        // 1. Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found for the provided email."));

        // 2. Check if the key status is ACTIVE
        if (user.getKeyStatus() != KeyStatus.ACTIVE) {
            throw new IllegalStateException("License is currently inactive or expired. Status: " + user.getKeyStatus());
        }

        // 3. Verify the provided plain key against the HASHED key stored in the database
        String storedHashedKey = user.getLicensekey();

        // This relies on your KeyGeneratorService having a method to check a plain string against a hash
        // IMPORTANT: Ensure keyGeneratorService.checkKey() is implemented (e.g., using BCrypt.checkpw)
        boolean isKeyValid = keyGeneratorService.checkKey(plainLicenseKey, storedHashedKey);

        if (!isKeyValid) {
            throw new IllegalStateException("Invalid license key provided.");
        }

        // 4. Check expiry time
        if (user.getKeyExpiryTime() != null && user.getKeyExpiryTime().isBefore(LocalDateTime.now())) {
            // Consider updating status to EXPIRED here, but throwing the exception is sufficient for login check
            throw new IllegalStateException("License has expired.");
        }

        return user; // Success: Key is valid and active
    }
}