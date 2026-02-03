package com.zrp.toyproject01.domain.account.dao;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.zrp.toyproject01.domain.account.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {
    // 이메일로 사용자 조회 (로그인 시 사용)
    Optional<User> findByEmail(String email);

    // 이메일 중복 가입 방지용
    boolean existsByEmail(String email);

}
