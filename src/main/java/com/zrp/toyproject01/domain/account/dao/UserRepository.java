package com.zrp.toyproject01.domain.account.dao;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.zrp.toyproject01.domain.account.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

}
