package com.example.loanmanagement.mapper;

import com.example.loanmanagement.dto.request.CreateLoanTypeRequest;
import com.example.loanmanagement.dto.request.UpdateLoanTypeRequest;
import com.example.loanmanagement.dto.response.LoanTypeResponse;
import com.example.loanmanagement.entity.LoanType;
import com.example.loanmanagement.enums.LoanTypeStatus;
import org.springframework.stereotype.Component;

@Component
public class LoanTypeMapper {

    public LoanTypeResponse toResponse(LoanType entity) {
        return LoanTypeResponse.builder()
                .id(entity.getId())
                .loanTypeCode(entity.getLoanTypeCode())
                .loanTypeName(entity.getLoanTypeName())
                .description(entity.getDescription())
                .minimumAmount(entity.getMinimumAmount())
                .maximumAmount(entity.getMaximumAmount())
                .annualInterestRate(entity.getAnnualInterestRate())
                .minimumTenureMonths(entity.getMinimumTenureMonths())
                .maximumTenureMonths(entity.getMaximumTenureMonths())
                .loanTypeStatus(entity.getLoanTypeStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public LoanType toEntity(CreateLoanTypeRequest req) {
        return LoanType.builder()
                .loanTypeCode(req.getLoanTypeCode().trim().toUpperCase())
                .loanTypeName(req.getLoanTypeName().trim())
                .description(req.getDescription())
                .minimumAmount(req.getMinimumAmount())
                .maximumAmount(req.getMaximumAmount())
                .annualInterestRate(req.getAnnualInterestRate())
                .minimumTenureMonths(req.getMinimumTenureMonths())
                .maximumTenureMonths(req.getMaximumTenureMonths())
                .loanTypeStatus(LoanTypeStatus.ACTIVE)
                .build();
    }

    public void updateEntity(LoanType entity, UpdateLoanTypeRequest req) {
        entity.setLoanTypeName(req.getLoanTypeName().trim());
        entity.setDescription(req.getDescription());
        entity.setMinimumAmount(req.getMinimumAmount());
        entity.setMaximumAmount(req.getMaximumAmount());
        entity.setAnnualInterestRate(req.getAnnualInterestRate());
        entity.setMinimumTenureMonths(req.getMinimumTenureMonths());
        entity.setMaximumTenureMonths(req.getMaximumTenureMonths());
    }
}