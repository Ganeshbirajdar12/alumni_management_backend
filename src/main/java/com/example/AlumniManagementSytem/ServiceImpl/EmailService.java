package com.example.AlumniManagementSytem.ServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    // ==================== OTP EMAIL ====================
    public void sendPromotionOtp(String toEmail, String studentName, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Verify Your Alumni Promotion - AlumniHub");
            message.setText(
                    "Hello " + studentName + ",\n\n" +
                            "You requested to be promoted to Alumni status.\n\n" +
                            "Your OTP is: " + otp + "\n\n" +
                            "This OTP is valid for 10 minutes.\n\n" +
                            "If you did not request this, please ignore this email.\n\n" +
                            "Best regards,\n" +
                            "AlumniHub Team"
            );

            mailSender.send(message);
            log.info("OTP email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send OTP email: " + e.getMessage());
        }
    }

    // ==================== PENDING APPROVAL EMAIL ====================
    public void sendPendingApprovalEmail(String toEmail, String studentName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Verification Complete - Awaiting Approval");
            message.setText(
                    "Hi " + studentName + ",\n\n" +
                            "Your email is verified! ✅\n\n" +
                            "Your promotion request is now with our admin team.\n" +
                            "You'll receive an email once it's approved (usually within 24 hours).\n\n" +
                            "Best regards,\n" +
                            "AlumniHub Team"
            );

            mailSender.send(message);
            log.info("Pending approval email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send pending approval email: {}", e.getMessage());
        }
    }

    // ==================== APPROVED EMAIL ====================
    public void sendApprovalEmail(String toEmail, String studentName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("🎉 Welcome to AlumniHub Alumni Community!");
            message.setText(
                    "Congratulations " + studentName + "!\n\n" +
                            "You are now an official ALUMNUS of AlumniHub.\n\n" +
                            "You can now:\n" +
                            "✓ Update work information\n" +
                            "✓ Post jobs\n" +
                            "✓ Mentor students\n" +
                            "✓ Join the alumni directory\n\n" +
                            "Login to explore your new features!\n\n" +
                            "Best regards,\n" +
                            "AlumniHub Team"
            );

            mailSender.send(message);
            log.info("Approval email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send approval email: {}", e.getMessage());
        }
    }

    // ==================== REJECTED EMAIL ====================
    public void sendRejectionEmail(String toEmail, String studentName, String reason) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Promotion Request Update - AlumniHub");
            message.setText(
                    "Hi " + studentName + ",\n\n" +
                            "Unfortunately, your promotion request was not approved.\n\n" +
                            "Reason: " + (reason != null ? reason : "Not specified") + "\n\n" +
                            "You can request again after 24 hours.\n\n" +
                            "Best regards,\n" +
                            "AlumniHub Team"
            );

            mailSender.send(message);
            log.info("Rejection email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send rejection email: {}", e.getMessage());
        }
    }

    // ==================== WELCOME EMAIL ====================
    public void sendAdminWelcomeEmail(String toEmail, String adminName, String tempPassword) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Welcome to AlumniHub Admin Team");
            message.setText(
                    "Hi " + adminName + ",\n\n" +
                            "You've been added as an ADMIN to AlumniHub.\n\n" +
                            "Your login credentials:\n" +
                            "    Email:    " + toEmail + "\n" +
                            "    Password: " + tempPassword + "\n\n" +
                            "⚠️  Please change your password after first login.\n\n" +
                            "You can now:\n" +
                            "✓ Approve alumni promotions\n" +
                            "✓ Manage users\n" +
                            "✓ Manage events and jobs\n\n" +
                            "Login at: http://localhost:5173/login\n\n" +
                            "Best regards,\n" +
                            "AlumniHub Team"
            );

            mailSender.send(message);
            log.info("Admin welcome email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send admin welcome email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send email");
        }
    }
}