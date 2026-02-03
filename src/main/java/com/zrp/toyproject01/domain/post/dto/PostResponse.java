package com.zrp.toyproject01.domain.post.dto;

import java.time.LocalDateTime;

import com.zrp.toyproject01.domain.post.domain.Post;

public record PostResponse(
    Long id,
    String title,
    String content,
    String author, // 작성자 닉네임 또는 이메일
    LocalDateTime updateedAt // 마지막 수정일
) {
    // Entity -> DTO 
    public static PostResponse from(Post post) {
        return new PostResponse(
            post.getId(),
            post.getTitle(),
            post.getContent(), 
            post.getUser().getNickname(),
            post.getUpdatedAt()
        );
    }
}
