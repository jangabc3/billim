package com.billim.service.resource;

import com.billim.common.Times;
import com.billim.domain.resource.FavoriteResource;
import com.billim.repository.FavoriteResourceRepository;
import com.billim.repository.PublicResourceRepository;
import com.billim.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FavoriteResourceService {

    private final FavoriteResourceRepository favoriteResourceRepository;
    private final PublicResourceRepository publicResourceRepository;
    private final UserRepository userRepository;

    public FavoriteResourceService(FavoriteResourceRepository favoriteResourceRepository,
            PublicResourceRepository publicResourceRepository,
            UserRepository userRepository) {
        this.favoriteResourceRepository = favoriteResourceRepository;
        this.publicResourceRepository = publicResourceRepository;
        this.userRepository = userRepository;
    }

    /** 로그인한 사용자의 즐겨찾기 목록 — 최근 추가한 순. */
    @Transactional(readOnly = true)
    public List<FavoriteResource> list(Long userId) {
        return favoriteResourceRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 즐겨찾기 추가. 이미 추가돼 있으면 조용히 무시한다(멱등성).
     * 중복 방지는 확인 후 저장이 아니라 DB의 on conflict do nothing으로 처리해서,
     * 하트를 연타하거나 요청이 동시에 와도 500이 나지 않는다.
     */
    @Transactional
    public void add(Long userId, Long publicResourceId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다: " + userId);
        }
        if (!publicResourceRepository.existsById(publicResourceId)) {
            throw new IllegalArgumentException("존재하지 않는 자원입니다: " + publicResourceId);
        }

        favoriteResourceRepository.insertIgnore(userId, publicResourceId, Times.now());
    }

    /** 즐겨찾기 취소. 이미 없어도 에러 없이 끝낸다(멱등성 — 두 번 눌러도 안전). */
    @Transactional
    public void remove(Long userId, Long publicResourceId) {
        favoriteResourceRepository.findByUserIdAndPublicResourceId(userId, publicResourceId)
                .ifPresent(favoriteResourceRepository::delete);
    }
}