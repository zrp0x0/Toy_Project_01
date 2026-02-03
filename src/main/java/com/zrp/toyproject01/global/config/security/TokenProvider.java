package com.zrp.toyproject01.global.config.security;

import java.security.Key;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import com.zrp.toyproject01.domain.account.domain.Role;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;

import java.util.Arrays;
import java.util.Collection;

@Component
public class TokenProvider {
    
    private final Key key;
    private final Long tokenValidityTime;

    // 1. 생성자: 설정 파일에서 비밀키를 가져와서 사용할 준비
    public TokenProvider(@Value("${jwt.secret}") String secretKey) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey); // 비밀키를 디코딩
        this.key = Keys.hmacShaKeyFor(keyBytes);            // 자바의 Key 객체로 교환
        this.tokenValidityTime = 1000L * 60 * 1;      // 토크 유효 시간 1분
    }

    // 2. 토큰 생성 (로그인 성공 시 호출)
    public String createToken(String email, Set<Role> roles, String nickname) {
        long now = (new Date()).getTime();
        Date validity = new Date(now + this.tokenValidityTime);

        String authorities = roles.stream()
                .map(Role::name)
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .setSubject(email)
                .claim("auth", authorities)
                .claim("nickname", nickname)
                .signWith(key, SignatureAlgorithm.HS512)
                .setExpiration(validity)
                .compact();
    }

    // 3. 토큰에서 이메일(사용자 정보) 꺼내기
    public String getEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject(); // 나중에 get("nickname", String.class)로 해서 닉네임을 뽑아올 수 있음
    }

    // 4. 유효성 검사 (위조나 만료 확인)
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            // 설명이 잘못됨 (위조)
        } catch (ExpiredJwtException e) {
            // 만료됨 (시간 초과)
        } catch (UnsupportedJwtException e) {
            // 지원하지 않는 토큰 방식
        } catch (IllegalArgumentException e) {
            // 토큰이 비어있음
        }
        return false;
    }

    // 5. 토큰에서 정보를 꺼내서 Spring Security 인증 객체(Authentication)을 만드는 과정
    public Authentication getAuthentication(String token) {

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        Collection<? extends GrantedAuthority> authorities = 
            Arrays.stream(claims.get("auth").toString().split(","))
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        User principal = new User(
            claims.getSubject(), "", authorities
        ); // username / password / 권한들

        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    // 리프레시 토큰 생성
    public String createRefreshToken(String email) {
        long now = (new Date()).getTime();
        Date validity = new Date(now + (1000L * 60 * 60 * 24)); // 1일

        return Jwts.builder()
            .setSubject(email)
            .setExpiration(validity)
            .signWith(key, SignatureAlgorithm.HS512)
            .compact();
    }

    // 토큰의 만료 시간을 꺼내는 메서드
    public Long getExpiration(String token) {
        Date expiration = Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .getBody()
            .getExpiration();

        return expiration.getTime();
    }

}
