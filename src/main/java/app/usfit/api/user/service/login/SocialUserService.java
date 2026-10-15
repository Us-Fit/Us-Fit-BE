package app.usfit.api.user.service.login;

import java.util.function.Supplier;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import app.usfit.api.common.enums.AuthProviderEnum;
import app.usfit.api.user.entity.User;
import app.usfit.api.user.repository.UserRepository;

/// <summary>
/// 소셜 로그인 사용자 조회/생성 (provider + providerId 기준)
/// </summary>
@Service
public class SocialUserService {

    private final UserRepository userRepository;

    public SocialUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // 첫 로그인 요청이 동시에 오면 둘 다 "없음"으로 조회될 수 있음
    // -> UNIQUE(provider, provider_id) 위반 시 먼저 생성된 계정을 다시 조회해서 반환
    // NOT_SUPPORTED: 바깥 트랜잭션에 묶이면 INSERT 실패 시 전체가 rollback-only가 되므로 분리
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public User findOrCreate(AuthProviderEnum provider, String providerId, Supplier<User> newUser) {
        return userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> {
                    try {
                        return userRepository.saveAndFlush(newUser.get());
                    } catch (DataIntegrityViolationException e) {
                        return userRepository.findByProviderAndProviderId(provider, providerId)
                                .orElseThrow(() -> e);
                    }
                });
    }
}
