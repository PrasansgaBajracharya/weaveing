package com.weaveing.repository;

import com.weaveing.entity.Pattern;
import com.weaveing.entity.User;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PatternRepository extends JpaRepository<Pattern, Long> {

    List<Pattern> findByCreator(User creator);

    List<Pattern> findByCreatorAndApprovalStatus(
            User creator,
            Pattern.ApprovalStatus approvalStatus
    );

    List<Pattern> findByApprovalStatus(
            Pattern.ApprovalStatus approvalStatus
    );

    List<Pattern> findTop12ByApprovalStatusOrderBySubmittedAtDesc(
            Pattern.ApprovalStatus approvalStatus
    );

    List<Pattern> findTop4ByApprovalStatusAndCategoryAndIdNotOrderBySubmittedAtDesc(
            Pattern.ApprovalStatus approvalStatus,
            String category,
            Long id
    );


    @Query("""
            SELECT p
            FROM Pattern p
            WHERE p.approvalStatus = :approvalStatus
              AND (
                    :search = ''
                    OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(p.category) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(p.creator.username) LIKE LOWER(CONCAT('%', :search, '%'))
              )
              AND (
                    :category = 'all'
                    OR (
                        :category = 'Amigurumi & Toys'
                        AND LOWER(p.category) IN ('amigurumi', 'animals', 'amigurumi & toys')
                    )
                    OR (
                        :category = 'Bags & Pouches'
                        AND LOWER(p.category) IN ('bags', 'bag', 'bags & pouches')
                    )
                    OR (
                        :category = 'Wearables'
                        AND LOWER(p.category) IN ('wearables', 'wearable')
                    )
                    OR (
                        :category = 'Home & Decor'
                        AND LOWER(p.category) IN ('home decor', 'home & decor', 'home & décor')
                    )
                    OR (
                        :category = 'Accessories'
                        AND LOWER(p.category) IN ('accessories', 'keychains', 'accessory')
                    )
                    OR (
                        :category = 'Flowers & Plants'
                        AND LOWER(p.category) IN ('flowers', 'flowers & plants')
                    )
                    OR (
                        :category = 'Seasonal & Gifts'
                        AND LOWER(p.category) IN ('seasonal', 'seasonal & gifts')
                    )
              )
              AND (
                    :difficulty = 'all'
                    OR LOWER(p.difficulty) = LOWER(:difficulty)
              )
              AND (
                    :priceType = 'all'
                    OR (:priceType = 'free' AND p.free = true)
                    OR (:priceType = 'paid' AND p.free = false)
              )
            """)
    List<Pattern> searchApprovedPatterns(
            @Param("approvalStatus") Pattern.ApprovalStatus approvalStatus,
            @Param("search") String search,
            @Param("category") String category,
            @Param("difficulty") String difficulty,
            @Param("priceType") String priceType,
            Sort sort
    );
}
