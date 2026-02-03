package com.zrp.toyproject01.domain.post.application;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.zrp.toyproject01.domain.account.dao.UserRepository;
import com.zrp.toyproject01.domain.account.domain.User;
import com.zrp.toyproject01.domain.post.dao.PostRepository;
import com.zrp.toyproject01.domain.post.domain.Post;
import com.zrp.toyproject01.domain.post.dto.PostResponse;
import com.zrp.toyproject01.domain.post.dto.PostSaveRequest;
import com.zrp.toyproject01.domain.post.dto.PostUpdateRequest;
import com.zrp.toyproject01.global.error.BusinessException;
import com.zrp.toyproject01.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본 일기 전용 (성능 최적화)
public class PostService {
 
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    
    /**
     * 게시글 작성
     * @Param email 로그인한 사용자의 이메일 (토큰에서 추출)
     */
    @Transactional
    public Long save(PostSaveRequest request, String email) {
        // 1. 작성자 찾기
        User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        // 2. 게시글 엔티티 생성
        Post post = Post.create(request.title(), request.content(), user);

        // 3. 저장 및 ID 반환
        return postRepository.save(post).getId();
    }

    /**
     * 게시글 수정
     * @Param email 수정하려는 사람의 이메일 (본인 확인용)
     */
    @Transactional
    public Long update(Long id, PostUpdateRequest request, String email) {
        Post post = postRepository.findById(id)
                        .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        
        // 본인 확인 로직 추가 기능
        if (!post.getUser().getEmail().equals(email)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        // dirty checking (자동 업데이트)
        // - JPA가 알아서 CCTV처럼 지켜보고 있다가, 뭔가 바뀌면 자동으로 DB를 고쳐주는 기능
        post.update(request.title(), request.content());
        return id;
    }

    /**
     * 게시글 단건 조회
     */
    public PostResponse findById(Long id) {
        Post post = postRepository.findById(id)
                    .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        return PostResponse.from(post);
    }

    /**
     * 게시글 전체 조회 (최신순)
     */
    public List<PostResponse> findAllDesc() {
        return postRepository.findAllDesc().stream()            
                .map(PostResponse::from) // Entity -> DTO 변환
                .collect(Collectors.toList());
    }

    /**
     * 게시글 삭제
     */
    @Transactional
    public void delete(Long id, String email) {
        Post post = postRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        // 본인 확인
        if (!post.getUser().getEmail().equals(email)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        postRepository.delete(post);
    }

}

// dirty checking
/*
### @Transactional 안에서만 일어남
- 조회: DB에서 데이터를 가져올 때, JPA는 이 데이터의 최초 상태를 자기만의 주머니(영속성 컨텍스트)에 복사해둠
- 수정: post.update(): Java 객체의 값만 바꿈
- 트랜잭션 커밋(메서드 종료):
    - JPA는 현재 객체와 아까 찍어둔 스냅샷을 비교
- 자동 반영(Flush)
    - 다른 점이 발견되면 쓰레기가 묻었네?하고 판단해서 자동으로 update 쿼리를 생성해서 DB에 날림


### 주의사항
- @Transactional이 필수!!!
- 영속 상태 엔티티만 가능
    - 방금 만든 객체나
    - DB에서 가져왔는데 연결이 끊긴 객체는 감시하지 않음
*/