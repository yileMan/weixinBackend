package com.man.backend.user.repository;

import com.man.backend.user.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByOpenid(String openid);

    boolean existsByUserId(String userId);
}
