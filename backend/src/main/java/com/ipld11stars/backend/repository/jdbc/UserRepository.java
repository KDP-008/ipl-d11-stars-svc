package com.ipld11stars.backend.repository.jdbc;

import com.ipld11stars.backend.model.User;
import com.ipld11stars.backend.model.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<User> userRowMapper = (ResultSet rs, int rowNum) -> {
        User user = new User();
        user.setId(rs.getString("id"));
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setUserId(rs.getString("user_id"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(UserRole.fromValue(rs.getString("role")));
        user.setStatus(rs.getString("status"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            user.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return user;
    };

    public Optional<User> findById(String id) {
        String sql = "SELECT * FROM ipld11stars.users WHERE id = ?";
        try {
            User user = jdbcTemplate.queryForObject(sql, userRowMapper, id);
            return Optional.of(user);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM ipld11stars.users WHERE email = ?";
        try {
            User user = jdbcTemplate.queryForObject(sql, userRowMapper, email);
            return Optional.of(user);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<User> findByUserId(String userId) {
        String sql = "SELECT * FROM ipld11stars.users WHERE user_id = ?";
        try {
            User user = jdbcTemplate.queryForObject(sql, userRowMapper, userId);
            return Optional.of(user);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public List<User> findAll() {
        String sql = "SELECT * FROM ipld11stars.users ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, userRowMapper);
    }

    public User save(User user) {
        if (user.getId() == null) {
            return insert(user);
        } else {
            return update(user);
        }
    }

    private User insert(User user) {
        String id = UUID.randomUUID().toString();
        String sql = "INSERT INTO ipld11stars.users (id, first_name, last_name, user_id, email, password_hash, role, status, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
        jdbcTemplate.update(sql, id, user.getFirstName(), user.getLastName(), user.getUserId(), user.getEmail(),
                           user.getPasswordHash(), user.getRole().getValue(), user.getStatus());
        user.setId(id);
        return user;
    }

    private User update(User user) {
        String sql = "UPDATE ipld11stars.users SET first_name = ?, last_name = ?, user_id = ?, email = ?, password_hash = ?, " +
                    "role = ?, status = ?, updated_at = NOW() WHERE id = ?";
        jdbcTemplate.update(sql, user.getFirstName(), user.getLastName(), user.getUserId(), user.getEmail(),
                           user.getPasswordHash(), user.getRole().getValue(), user.getStatus(), user.getId());
        return user;
    }

    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM ipld11stars.users WHERE email = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email);
        return count != null && count > 0;
    }

    public boolean existsByUserId(String userId) {
        String sql = "SELECT COUNT(*) FROM ipld11stars.users WHERE user_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId);
        return count != null && count > 0;
    }

    public void delete(String id) {
        String sql = "DELETE FROM ipld11stars.users WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
}
