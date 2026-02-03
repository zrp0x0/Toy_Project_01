package com.zrp.toyproject01.domain.post.domain;

import com.zrp.toyproject01.domain.account.domain.User;
import com.zrp.toyproject01.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    // TEXT 타입으로 지정 (본문은 길 수 있으니깐)
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // ==========================================
    // [핵심] 연관관계 매핑 (User <-> Post)
    // ==========================================
    // 1. Post 입장에서는 작성자(User)가 한 명입니다. (Many Posts To One User)
    // 2. FetchType.LAZY: "지연 로딩"
    //    -> 게시글을 조회할 때 작성자 정보를 당장 가져오지 않고, 진짜 필요할 때 쿼리를 날립니다.
    //    -> 실무 성능 최적화의 필수 규칙입니다! (EAGER 쓰면 나중에 DB 폭발함)
    @ManyToOne(fetch = FetchType.LAZY) // 유저 정보가 필요하면 그 때 가져옴
    @JoinColumn(name = "user_id")
    private User user;

    
    // 생성자 - 팩토리 메소드
    private Post(String title, String content, User user) {
        this.title = title;
        this.content = content;
        this.user = user;
    }

    public static Post create(String title, String content, User user) {
        return new Post(title, content, user);
    }

    // Setter를 남용하지 말고 명확한 목적의 메소드를 만듦
    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

}
