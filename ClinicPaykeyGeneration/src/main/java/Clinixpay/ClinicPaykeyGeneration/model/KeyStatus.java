package Clinixpay.ClinicPaykeyGeneration.model;

public enum KeyStatus {
    ACTIVE,
    PENDING_PAYMENT, // New status
    EXPIRED,
    REVOKED
}