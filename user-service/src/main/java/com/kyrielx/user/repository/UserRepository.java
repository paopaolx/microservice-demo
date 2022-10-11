package com.kyrielx.user.repository;

import com.kyrielx.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author lixing
 * @date 2022/10/8
 */
@Repository
public interface UserRepository extends JpaRepository<Users, Long> {
    Users findByUserId(Long userId);
}
