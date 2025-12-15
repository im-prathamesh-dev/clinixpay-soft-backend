package Clinixpay.ClinicPaykeyGeneration.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender; // Spring's native mail sender

    // Inject the configured email sender from application.properties
    @Value("${smtp.email.from}")
    private String fromEmail;

    // Constructor Injection
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends a formal letter upon successful registration with the login key and terms using SMTP.
     */
    public void sendPaymentSuccessEmail(String toEmail, String fullName, String licensekey, String planName, Long amountPaise) {

        try {
            // 1. Create a MimeMessage for complex content (like HTML)
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true); // 'true' enables multipart/HTML content

            // Convert paise back to rupees for the message
            String amountRupees = String.format("%.2f", amountPaise / 100.0);
            String subject = "Payment Successful & Welcome to Clinixpay!";
            String emailHtmlBody = createHtmlEmailBody(fullName, licensekey, planName, amountRupees);

            // 2. Set message properties
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(emailHtmlBody, true); // 'true' indicates the text is HTML

            // 3. Send the email
            mailSender.send(message);

            System.out.println("Login key email sent successfully via SMTP to: " + toEmail);

        } catch (Exception e) {
            // Rethrow as a RuntimeException to be caught by the RegistrationService's error handler
            throw new RuntimeException("SMTP Email failed for user " + toEmail + ". Error: " + e.getMessage(), e);
        }
    }

    // Helper method to create a professional HTML-formatted email body
    private String createHtmlEmailBody(String fullName, String licensekey, String planName, String amountRupees) {
        // Use inline styles for maximum compatibility across email clients
        return """
        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; border: 1px solid #ddd; padding: 20px;">
            <header style="background-color: #007BFF; color: white; padding: 15px; text-align: center;">
                <h2>CLINIXPAY - OFFICIAL CONFIRMATION</h2>
            </header>
            
            <section style="padding: 20px 0;">
                <p>Dear <strong>""" + fullName + """
                </strong>,</p>
                
                <p>We are delighted to confirm your registration and the successful processing of your payment for the <strong>""" + planName + """
                </strong> plan.</p>
                <p style="font-size: 1.1em; color: #28A745; font-weight: bold;">Amount Paid: ₹""" + amountRupees + """
                </p>
                <p>Your account is now <strong>ACTIVE</strong>.</p>
                
                <div style="border: 2px solid #FFC107; padding: 15px; text-align: center; margin: 20px 0; background-color: #FFFBEA;">
                    <h3 style="margin-top: 0; color: #333;">Your Secure Login Key</h3>
                    <p style="font-size: 1.5em; font-weight: bold; color: #FFC107; margin: 5px 0;">""" + licensekey + """
                    </p>
                </div>
                
                <p>Please use this key immediately to log in to our services. For security purposes, this key is valid for **7 days** from the moment your payment was captured.</p>
            </section>
            
            <section style="border-top: 1px solid #ddd; padding-top: 20px;">
                <h4 style="color: #6C757D;">Terms and Conditions Summary</h4>
                <ul style="color: #555; line-height: 1.6;">
                    <li><strong>Key Validity:</strong> The generated key is valid for a period of 7 days.</li>
                    <li><strong>Refunds:</strong> All sales are final. No refunds will be issued after the service has been activated.</li>
                    <li><strong>Security:</strong> You are responsible for maintaining the confidentiality of your login key.</li>
                </ul>
            </section>
            
            <footer style="margin-top: 30px; text-align: center; font-size: 0.8em; color: #6C757D;">
                <p>Thank you for choosing Clinixpay.</p>
                <p>CLINIXPAY SUPPORT TEAM</p>
            </footer>
        </div>
        """;
    }
}