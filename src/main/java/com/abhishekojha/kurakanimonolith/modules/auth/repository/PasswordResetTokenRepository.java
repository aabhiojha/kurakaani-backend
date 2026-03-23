package com.abhishekojha.kurakanimonolith.modules.auth.repository;

import com.abhishekojha.kurakanimonolith.modules.auth.model.PasswordResetToken;
import com.abhishekojha.kurakanimonolith.modules.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    List<PasswordResetToken> findByUser(User user);

    Optional<PasswordResetToken> findFirstByUserOrderByIdDesc(User user);

    PasswordResetToken findByToken(Integer token);

    List<PasswordResetToken> findByUserAndUsedNot(User user, Boolean used);

    List<PasswordResetToken> findByUserAndUsed(User user, Boolean used);
}
