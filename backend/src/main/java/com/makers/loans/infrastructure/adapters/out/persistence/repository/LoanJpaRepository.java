package com.makers.loans.infrastructure.adapters.out.persistence.repository;

import com.makers.loans.infrastructure.adapters.out.persistence.entity.LoanEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoanJpaRepository extends JpaRepository<LoanEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "user")
    Optional<LoanEntity> findById(Long id);

    @EntityGraph(attributePaths = "user")
    List<LoanEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = "user")
    List<LoanEntity> findAllByOrderByCreatedAtDesc();

    boolean existsByUserId(Long userId);
}
