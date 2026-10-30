package com.lottery.auth.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
@SuppressWarnings({"SqlNoDataSourceInspection", "SqlResolve"})
@Repository
public class UserRoleRepository {

    @PersistenceContext
    private EntityManager em;

    @SuppressWarnings("unchecked")
    public List<String> findRoleNames(Long userId) {
        return em.createNativeQuery(
                "select r.name from roles r " +
                "join user_roles ur on ur.role_id = r.id " +
                "where ur.user_id = :uid")
                .setParameter("uid", userId)
                .getResultList();
    }

    public void assign(Long userId, String roleName) {
        int rows = em.createNativeQuery(
                "insert into user_roles (user_id, role_id) " +
                "select :uid, r.id from roles r where r.name = :name")
                .setParameter("uid", userId)
                .setParameter("name", roleName)
                .executeUpdate();
        if (rows == 0) {
            throw new IllegalStateException("Role not found: " + roleName);
        }
    }
}