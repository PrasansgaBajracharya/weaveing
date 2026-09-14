package com.weaveing.repository;

import com.weaveing.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByVerificationToken(String verificationToken);

    Optional<User> findByEmailOrUsername(String email, String username);

    List<User> findByAdminTrue();

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}