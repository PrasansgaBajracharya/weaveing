package com.weaveing.repository;

import com.weaveing.entity.TrustedDevice;
import com.weaveing.entity.User;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TrustedDeviceRepository
        extends JpaRepository<TrustedDevice, Long> {

    Optional<TrustedDevice> findByToken(String token);

    @Transactional
    void deleteByUser(User user);
}
