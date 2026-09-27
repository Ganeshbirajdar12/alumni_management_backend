package com.example.AlumniManagementSytem.Repository;

import com.example.AlumniManagementSytem.Model.PromotionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRequestRepository extends JpaRepository<PromotionRequest, Long> {

    // Find latest request for a user (any status)
    Optional<PromotionRequest> findTopByUserIdOrderByRequestedAtDesc(Long userId);

    // Find latest request by status
    Optional<PromotionRequest> findTopByUserIdAndStatusOrderByRequestedAtDesc(Long userId, String status);

    // Find all pending approvals (for admin)
    List<PromotionRequest> findByStatusOrderByRequestedAtAsc(String status);

    // Check if user has an active request
    boolean existsByUserIdAndStatusIn(Long userId, List<String> statuses);
}