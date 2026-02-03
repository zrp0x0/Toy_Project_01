package com.zrp.toyproject01.domain.account.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zrp.toyproject01.domain.account.domain.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    Optional<RefreshToken> findByKey(String key);
}
