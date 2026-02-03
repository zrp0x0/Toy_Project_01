package com.zrp.toyproject01.domain.post.api;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zrp.toyproject01.domain.post.application.PostService;
import com.zrp.toyproject01.domain.post.dto.PostResponse;
import com.zrp.toyproject01.domain.post.dto.PostSaveRequest;
import com.zrp.toyproject01.domain.post.dto.PostUpdateRequest;
import com.zrp.toyproject01.global.common.ApiResponse;
import com.zrp.toyproject01.global.util.SecurityUtil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {
    
    private final PostService postService;

    /**
     * 게시글 작성
     * 로그인한 사용자만 가능
     */
    @PostMapping
    public ApiResponse<Long> save(
        @RequestBody @Valid PostSaveRequest request
    ) {
        String email = SecurityUtil.getCurrentUserEmail();

        Long postId = postService.save(request, email);
        return ApiResponse.ok(postId);
    }

    /**
     * 게시글 수정
     * 작성자 본인만 가능
     */
    @PutMapping("/{id}")
    public ApiResponse<Long> update(
        @PathVariable Long id, 
        @RequestBody @Valid PostUpdateRequest request
    ) {
        String email = SecurityUtil.getCurrentUserEmail();
        System.out.println("fucking: " + email);

        Long postId = postService.update(id, request, email);
        return ApiResponse.ok(postId);
    }

    /**
     * 게시글 삭제
     * 작성자 본인만 가능
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
        @PathVariable("id") Long id
    ) {
        String email = SecurityUtil.getCurrentUserEmail();

        postService.delete(id, email);
        return ApiResponse.ok();
    }

    /**
     * 게시글 단건 조회
     * 누구나 가능 (로그인 X)
     */
    @GetMapping("/{id}")
    public ApiResponse<PostResponse> findById(
        @PathVariable("id") Long id
    ) {
        return ApiResponse.ok(postService.findById(id));
    }

    /**
     * 게시글 전체 조회
     * 누구나 가능 (로그인 X)
     */
    @GetMapping
    public ApiResponse<List<PostResponse>> findAll() {
        return ApiResponse.ok(postService.findAllDesc());
    }

}
