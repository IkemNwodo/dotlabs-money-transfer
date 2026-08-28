package com.dotlabs.moneytransfer.repository.specification;

import com.dotlabs.moneytransfer.dto.request.TransactionFilterRequest;
import com.dotlabs.moneytransfer.entity.Transaction;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public final class TransactionSpecification {

    private TransactionSpecification() {
        // Utility class
    }

    public static Specification<Transaction> withFilter(TransactionFilterRequest filter) {
        return (root, query, criteriaBuilder) -> {
            if (filter == null) {
                return criteriaBuilder.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            // Filter by Status
            if (filter.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), filter.getStatus()));
            }

            // Filter by Account Number (matches either sender or recipient)
            if (filter.getAccountNumber() != null && !filter.getAccountNumber().trim().isEmpty()) {
                String accNum = filter.getAccountNumber().trim();
                Predicate sourceMatch = criteriaBuilder.equal(root.get("sourceAccountNumber"), accNum);
                Predicate destMatch = criteriaBuilder.equal(root.get("destinationAccountNumber"), accNum);
                predicates.add(criteriaBuilder.or(sourceMatch, destMatch));
            }

            // Filter by Start Date
            if (filter.getStartDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("dateCreated"),
                        filter.getStartDate().atStartOfDay()
                ));
            }

            // Filter by End Date
            if (filter.getEndDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("dateCreated"),
                        filter.getEndDate().atTime(LocalTime.MAX)
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
