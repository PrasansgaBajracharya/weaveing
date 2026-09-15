package com.weaveing.repository;

import com.weaveing.entity.User;
import com.weaveing.entity.Withdrawal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WithdrawalRepository extends JpaRepository<Withdrawal, Long> {

    List<Withdrawal> findByUserOrderByRequestedAtDesc(User user);

    List<Withdrawal> findByUserAndStatus(
            User user,
            Withdrawal.Status status
    );
}
