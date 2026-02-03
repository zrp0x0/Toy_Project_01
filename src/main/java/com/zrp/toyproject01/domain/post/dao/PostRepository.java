package com.zrp.toyproject01.domain.post.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.zrp.toyproject01.domain.post.domain.Post;

public interface PostRepository extends JpaRepository<Post, Long> {
    
    // 핵심 기능: 게시글 전체 조회 (최신순)
    @Query("select p from Post p join fetch p.user u order by p.createdAt desc")
    List<Post> findAllDesc();
}

// List<Post> findAllDesc에서 발생할 수 있는 문제
/*
### 그냥 findAll()을 사용한다면?
- Post 엔티티의 User를 LAZY(지연 로딩)으로 설정
- 게시글이 10라면?
    - 게시글 10개를 가져오는 쿼리 1방
    - 작성자 이름이 필요해지면 유저 정보를 가져오는 쿼리 10방
    - 총 1 + 11 = 12방 쿼리가 나감


### join fetch를 사용
- 게시글을 가져올 때, 유저 정보도 같이 가져와
- 게시글 10개 + 작성자 10명의 정보를 단 1방의 쿼리로 모두 가져옴
*/