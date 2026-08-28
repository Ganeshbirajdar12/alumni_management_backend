package com.example.AlumniManagementSytem.Model;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_token", columnList = "token"),
        @Index(name = "idx_refresh_token_user", columnList = "user_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 500)  // Reduced from 1000 to 500
    private String token;

    @Column(nullable = false)
    private Instant expiryDate;

    @Column(nullable = false, length = 50)
    private String tokenType;

    @Column(name = "is_revoked")
    private Boolean isRevoked = false;

    @Column(name = "is_used")
    private Boolean isUsed = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (isRevoked == null) isRevoked = false;
        if (isUsed == null) isUsed = false;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiryDate);
    }

    public boolean isValid() {
        return !isExpired() && Boolean.FALSE.equals(isRevoked) && Boolean.FALSE.equals(isUsed);
    }
}