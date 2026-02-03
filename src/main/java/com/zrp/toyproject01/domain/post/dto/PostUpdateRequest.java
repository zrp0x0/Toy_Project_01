package com.zrp.toyproject01.domain.post.dto;

import jakarta.validation.constraints.NotBlank;

// 게시글 수정 요청
public record PostUpdateRequest(
    @NotBlank (message = "제목은 필수입니다.")
    String title,

    @NotBlank (message = "내용은 필수입니다.")
    String content
) {}
