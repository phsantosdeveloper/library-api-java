package com.example.library.specifications;

import com.example.library.entities.Loan;
import com.example.library.entities.enums.LoanStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class LoanSpecifications {

    private LoanSpecifications() {
    }

    public static Specification<Loan> filter(String email, LoanStatus status) {
        Specification<Loan> spec = Specification.unrestricted();

        if (StringUtils.hasText(email)) spec = spec.and(userEmailEquals(email));
        if (status != null)             spec = spec.and(statusEquals(status));

        return spec;
    }

    private static Specification<Loan> userEmailEquals(String email) {
        return (root, query, cb) ->
                cb.equal(cb.upper(root.get("user").get("email")), email.trim().toUpperCase());
    }

    private static Specification<Loan> statusEquals(LoanStatus status) {
        return (root, query, cb) ->
                cb.equal(root.get("status"), status);
    }
}
