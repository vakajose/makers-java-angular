package com.makers.loans.infrastructure.adapters.out.persistence;

import com.makers.loans.application.ports.out.LoanRepositoryPort;
import com.makers.loans.domain.exception.UserNotFoundException;
import com.makers.loans.domain.model.Loan;
import com.makers.loans.infrastructure.adapters.out.persistence.entity.LoanEntity;
import com.makers.loans.infrastructure.adapters.out.persistence.entity.UserEntity;
import com.makers.loans.infrastructure.adapters.out.persistence.mapper.LoanPersistenceMapper;
import com.makers.loans.infrastructure.adapters.out.persistence.repository.LoanJpaRepository;
import com.makers.loans.infrastructure.adapters.out.persistence.repository.UserJpaRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class LoanPersistenceAdapter implements LoanRepositoryPort {

    private final LoanJpaRepository loanJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final LoanPersistenceMapper mapper;

    public LoanPersistenceAdapter(
            LoanJpaRepository loanJpaRepository,
            UserJpaRepository userJpaRepository,
            LoanPersistenceMapper mapper) {
        this.loanJpaRepository = loanJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    @CacheEvict(value = "loans", allEntries = true)
    public Loan save(Loan loan) {
        UserEntity userEntity = userJpaRepository.findById(loan.getUserId())
                .orElseThrow(() -> new UserNotFoundException(loan.getUserId()));
        LoanEntity entity = mapper.toEntity(loan, userEntity);
        LoanEntity saved = loanJpaRepository.saveAndFlush(entity);
        return mapper.toDomain(saved);
    }

    @Override
    @Cacheable(value = "loans", key = "'id:' + #id")
    public Optional<Loan> findById(Long id) {
        return loanJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Cacheable(value = "loans", key = "'user:' + #userId")
    public List<Loan> findByUserId(Long userId) {
        return loanJpaRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Cacheable(value = "loans", key = "'all'")
    public List<Loan> findAll() {
        return loanJpaRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByUserId(Long userId) {
        return loanJpaRepository.existsByUserId(userId);
    }
}
