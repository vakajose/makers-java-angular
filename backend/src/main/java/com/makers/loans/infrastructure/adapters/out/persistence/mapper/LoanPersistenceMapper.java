package com.makers.loans.infrastructure.adapters.out.persistence.mapper;

import com.makers.loans.domain.model.Loan;
import com.makers.loans.infrastructure.adapters.out.persistence.entity.LoanEntity;
import com.makers.loans.infrastructure.adapters.out.persistence.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class LoanPersistenceMapper {

    public Loan toDomain(LoanEntity entity) {
        return Loan.reconstitute(
                entity.getId(),
                entity.getUser().getId(),
                entity.getUser().getEmail(),
                entity.getAmount(),
                entity.getTermMonths(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }

    public LoanEntity toEntity(Loan domain, UserEntity userEntity) {
        LoanEntity entity = new LoanEntity();
        entity.setId(domain.getId());
        entity.setUser(userEntity);
        entity.setAmount(domain.getAmount());
        entity.setTermMonths(domain.getTermMonths());
        entity.setStatus(domain.getStatus());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setVersion(domain.getVersion());
        return entity;
    }
}
