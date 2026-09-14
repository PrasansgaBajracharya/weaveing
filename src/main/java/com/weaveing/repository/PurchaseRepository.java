// weave.ing purchase database queries.

package com.weaveing.repository;

import com.weaveing.entity.Pattern;
import com.weaveing.entity.Purchase;
import com.weaveing.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PurchaseRepository
        extends JpaRepository<Purchase, Long> {

    List<Purchase>
    findByBuyerAndPaymentStatusOrderByPurchasedAtDesc(
            User buyer,
            Purchase.PaymentStatus paymentStatus
    );

    Optional<Purchase>
    findByBuyerAndPatternAndPaymentStatus(
            User buyer,
            Pattern pattern,
            Purchase.PaymentStatus paymentStatus
    );

    boolean
    existsByBuyerAndPatternAndPaymentStatus(
            User buyer,
            Pattern pattern,
            Purchase.PaymentStatus paymentStatus
    );

    long
    countByPatternAndPaymentStatus(
            Pattern pattern,
            Purchase.PaymentStatus paymentStatus
    );

    List<Purchase>
    findByPatternAndPaymentStatus(
            Pattern pattern,
            Purchase.PaymentStatus paymentStatus
    );
}