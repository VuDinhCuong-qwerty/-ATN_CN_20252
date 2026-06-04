package com.iam.app.repository.jpa;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.iam.app.domain.AuthApplication;
import com.iam.app.domain.AuthClientGroup;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.criteria.Root;

public class ApplicationSpec {

    public static Specification<AuthApplication> withFilters(
            String type, String status, String serviceCode, String name, String groupName) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (type != null && !type.isBlank())
                predicates.add(cb.equal(root.get("appType"), type));

            if (status != null && !status.isBlank())
                predicates.add(cb.equal(root.get("status"), status));

            if (serviceCode != null && !serviceCode.isBlank())
                predicates.add(cb.like(cb.upper(root.get("serviceCode")),
                        "%" + serviceCode.toUpperCase() + "%"));

            if (name != null && !name.isBlank())
                predicates.add(cb.like(cb.upper(root.get("name")),
                        "%" + name.toUpperCase() + "%"));

            if (groupName != null && !groupName.isBlank()) {
                Subquery<Long> sub = query.subquery(Long.class);
                Root<AuthClientGroup> groupRoot = sub.from(AuthClientGroup.class);
                sub.select(groupRoot.get("id"))
                        .where(cb.like(cb.upper(groupRoot.get("name")),
                                "%" + groupName.toUpperCase() + "%"));
                predicates.add(root.get("groupId").in(sub));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
