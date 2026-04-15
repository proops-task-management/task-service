package com.proops2026.taskservice.repository.impl;

import com.proops2026.taskservice.model.Task;
import com.proops2026.taskservice.repository.TaskRepositoryCustom;
import com.proops2026.taskservice.service.TaskFilters;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class TaskRepositoryImpl implements TaskRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Task> findTasks(String userId, String role, TaskFilters filters) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Task> cq = cb.createQuery(Task.class);
        Root<Task> root = cq.from(Task.class);

        List<Predicate> predicates = buildPredicates(cb, root, userId, role, filters);
        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.desc(root.get("createdAt")));

        TypedQuery<Task> query = entityManager.createQuery(cq);
        return query.getResultList();
    }

    private List<Predicate> buildPredicates(
            CriteriaBuilder cb,
            Root<Task> root,
            String userId,
            String role,
            TaskFilters filters) {
        List<Predicate> predicates = new ArrayList<>();

        if (!"lead".equals(role)) {
            predicates.add(cb.or(
                    cb.equal(root.get("createdBy"), userId),
                    cb.equal(root.get("assigneeId"), userId)
            ));
        }

        if (filters.getAssigneeId() != null) {
            predicates.add(cb.equal(root.get("assigneeId"), filters.getAssigneeId()));
        }

        if (filters.getStatus() != null) {
            predicates.add(cb.equal(root.get("status"), filters.getStatus()));
        }

        if (filters.isOverdue()) {
            predicates.add(overduePredicate(cb, root));
        }

        return predicates;
    }

    private Predicate overduePredicate(CriteriaBuilder cb, Root<Task> root) {
        return cb.and(
                cb.isNotNull(root.get("dueDate")),
                cb.lessThan(root.get("dueDate"), LocalDate.now()),
                cb.notEqual(root.get("status"), "done")
        );
    }
}

