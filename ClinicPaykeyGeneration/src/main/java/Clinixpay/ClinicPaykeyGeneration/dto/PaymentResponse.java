package Clinixpay.ClinicPaykeyGeneration.dto;

import lombok.Data;

@Data
public class PaymentResponse {
    private String userId;
    private String orderId;
    private Long amountInPaise;
    private String currency = "INR";
    private String razorpayKeyId; // Sent to the frontend for payment popup
}