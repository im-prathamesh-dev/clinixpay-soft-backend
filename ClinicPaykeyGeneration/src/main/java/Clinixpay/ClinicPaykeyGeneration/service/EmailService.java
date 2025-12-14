package Clinixpay.ClinicPaykeyGeneration.service;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Sends the generated 12-digit key to the user's email.
     */
    public void sendLoginKeyEmail(String toEmail, String loginKey) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Your Secure Login Key");

        String emailBody = String.format(
                "Hello!\n\n" +
                        "Thank you for registering. Your unique login key is:\n\n" +
                        "=========================================\n" +
                        "LOGIN KEY: %s\n" +
                        "=========================================\n\n" +
                        "Please use this key to log in. It is valid for 7 days.\n\n" +
                        "Do not share this key.",
                loginKey
        );

        message.setText(emailBody);
        mailSender.send(message);
        System.out.println("Login key email sent successfully to: " + toEmail);
    }
}