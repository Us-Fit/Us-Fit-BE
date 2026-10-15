package app.usfit.api.RecruitPlayer.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import app.usfit.api.RecruitPlayer.dto.ApplicationApplyRequest;
import app.usfit.api.RecruitPlayer.dto.ApplicationPostResponse;
import app.usfit.api.RecruitPlayer.dto.ApplicationResponse;
import app.usfit.api.RecruitPlayer.dto.ApplicationStatusRequest;
import app.usfit.api.RecruitPlayer.dto.RecruitPostResponse;
import app.usfit.api.RecruitPlayer.entity.RecruitApplication;
import app.usfit.api.RecruitPlayer.entity.RecruitPlayerPost;
import app.usfit.api.RecruitPlayer.repository.RecruitApplicationRepository;
import app.usfit.api.RecruitPlayer.repository.RecruitPlayerPostRepository;
import app.usfit.api.user.entity.User;
import app.usfit.api.user.service.ProfileService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class RecruitApplicationService {
    private final RecruitApplicationRepository applicationRepo;
    private final RecruitPlayerPostRepository postRepo;
    private final ProfileService profileService;

    // persistenceContext -> EntityManager를 주입받기 위한 어노테이션 (우체국 직원 느낌.)
    // EntityManager -> JPA의 핵심 인터페이스로, 엔티티의 생명주기 관리, 쿼리 실행 등을 담당 (JPA에서 DB와 소통)
    @PersistenceContext 
    private EntityManager entityManager;

    public RecruitApplicationService(RecruitApplicationRepository recruitApplicationRepository,
            RecruitPlayerPostRepository recruitPlayerPostService, ProfileService profileService) {
        this.applicationRepo = recruitApplicationRepository;
        this.postRepo = recruitPlayerPostService;
        this.profileService = profileService;
    }

    @Transactional
    public ApplicationResponse apply(Long postId, Long applicantId, ApplicationApplyRequest req) {
        RecruitPlayerPost post = postRepo.findById(postId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 모집글"));
        
        if (post.getWriter().getId().equals(applicantId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "본인의 모집글에는 지원할 수 없습니다.");
        }

        if (applicationRepo.existsByPost_IdAndApplicant_Id(postId, applicantId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 지원한 모집글입니다.");
        }

        RecruitApplication app = new RecruitApplication();
        app.setPost(post);
        app.setApplicant(entityManager.getReference(User.class, applicantId));
        app.setIntroduction(req == null ? null : req.introduction());

        // 동시에 같은 지원이 들어오면 exists 검사를 둘 다 통과할 수 있음 -> UNIQUE(post_id, user_id)로 최종 차단 (409)
        RecruitApplication saved;
        try {
            saved = applicationRepo.saveAndFlush(app);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 지원한 모집글입니다.");
        }
        return toResponse(saved);
    }

    // 엔티티 -> 응답 DTO 매핑 헬퍼
    private ApplicationResponse toResponse(RecruitApplication a) {
        var userProfile = profileService.getSimpleProfile(a.getApplicant().getId());
        
        return new ApplicationResponse(
            a.getApplicationId(),
            a.getPost().getId(),
            userProfile,
            a.getIntroduction(),
            a.getStatus(),
            a.getAppliedAt()
        );
    }
    private ApplicationPostResponse toPostResponse(RecruitApplication a) {
        var userProfile = profileService.getSimpleProfile(a.getApplicant().getId());

        var post = a.getPost();
        var sportName = post.getSport() != null ? post.getSport().getName() : null;

        RecruitPostResponse RecruitPostResponse = new RecruitPostResponse(
            sportName,
            post.getTitle(),
            post.getDescription(),
            post.getFacilityId() != null ? post.getFacilityId() : null,
            post.getSidoNm(),
            post.getSigunguNm(),
            post.getActivityDurationMinutes()
        );

        return new ApplicationPostResponse(
            a.getApplicationId(),
            RecruitPostResponse,
            userProfile,
            a.getIntroduction(),
            a.getStatus(),
            a.getAppliedAt()
        );
    }

    // 게시글 신청 목록(작성자만 조회 가능)
    public List<ApplicationResponse> listForPost(Long postId, Long requesterId) {
        RecruitPlayerPost post = postRepo.findById(postId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 모집글"));
        if (!post.getWriter().getId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "작성자만 조회 가능합니다.");
        }
        return applicationRepo.findByPost_IdOrderByApplicationIdDesc(postId)
                .stream().map(this::toResponse).toList();
    }

    // 상태 변경(작성자만) - ACCEPTED / REJECTED
    // requsterId : 작성자 아이디
    // applicationId : 신청서 아이디
    @Transactional
    public ApplicationResponse changeStatus(Long postId, Long applicationId, Long requesterId, ApplicationStatusRequest req) {
        // 게시글 존재 여부 및 작성자 확인
        var post = postRepo.findById(postId).orElseThrow(() -> 
            new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 모집글"));
        
        if (!post.getWriter().getId().equals(requesterId)) {
            System.out.println("작성자만 변경 가능합니다.");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "작성자만 변경 가능합니다.");
        }

        // 신청서 존재 여부 확인
        var app = applicationRepo.findById(applicationId).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 신청서"));

        if (!app.getPost().getId().equals(postId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "해당 모집글의 신청서가 아닙니다.");
        }

        var from = app.getStatus();
        var to = req.status();
        if (from == to) {
            return toResponse(app);
        }

        // 1) 신청서 상태 전이 (조건부 UPDATE)
        //    같은 신청서를 동시에 수락하면 둘 다 WAITING을 읽고 자리를 두 번 확보할 수 있음 -> 먼저 전이한 요청만 통과
        if (applicationRepo.transitionStatus(applicationId, from, to) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "다른 요청에 의해 신청서 상태가 이미 변경되었습니다.");
        }

        // 2) 정원 확보/반납 (조건부 UPDATE)
        //    정원 마감 시 예외 -> 트랜잭션 롤백으로 1)의 상태 전이도 함께 취소됨
        boolean wasAccepted = from == RecruitApplication.Status.ACCEPTED;
        boolean willBeAccepted = to == RecruitApplication.Status.ACCEPTED;

        if (!wasAccepted && willBeAccepted) {
            if (postRepo.reserveSeat(postId) == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "모집 정원이 가득 찼습니다.");
            }
        } else if (wasAccepted && !willBeAccepted) {
            postRepo.releaseSeat(postId);
        }

        // 벌크 UPDATE는 영속성 컨텍스트에 반영되지 않으므로 응답용 엔티티 값만 맞춰줌
        app.setStatus(to);
        return toResponse(app);
    }


    // 내가 신청한 모집글 조회
    @Transactional
    public List<ApplicationPostResponse> listMyApplications(Long userId) {
        return applicationRepo.findByApplicant_IdOrderByApplicationIdDesc(userId)
                .stream().map(this::toPostResponse).toList();
    }

    // 멤버용: ACCEPTED만
    public List<ApplicationResponse> listForPostMember(Long postId, Long requesterId) {
        RecruitPlayerPost post = postRepo.findById(postId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 모집글"));
        
        List<ApplicationResponse> response = AcceptedApplications(postId);

        // 작성자 또는 ACCEPTED된 멤버인지 확인
        boolean requesterIsAccepted = response.stream()
                .anyMatch(a -> a.user() != null && a.user().userId().equals(requesterId)) || post.getWriter().getId().equals(requesterId);

        if (!requesterIsAccepted) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "작성자 또는 가입된 멤버만 조회 가능합니다.");
        }

        return response;
    }

    // ACCEPT된 멤버가 스스로 용병 탈퇴
    @Transactional
    public ApplicationResponse exitRecruit(Long postId, Long applicationId, Long requesterId) {
        RecruitPlayerPost post = postRepo.findById(postId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 모집글"));

        RecruitApplication app = applicationRepo.findById(applicationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 신청서"));

        // 신청서가 해당 게시글의 신청서인지 확인
        if (!app.getPost().getId().equals(postId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "해당 모집글의 신청서가 아닙니다.");
        }

        // 요청자가 신청자 본인인지 확인
        if (app.getApplicant() == null || !app.getApplicant().getId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "자기 자신만 탈퇴할 수 있습니다.");
        }

        // ACCEPTED(가입된 상태)일 때만 CANCELLED로 전이 + 자리 반납
        // 조건부 UPDATE라 탈퇴 요청이 중복으로 와도 자리는 한 번만 반납됨
        if (applicationRepo.transitionStatus(applicationId,
                RecruitApplication.Status.ACCEPTED, RecruitApplication.Status.CANCELLED) == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "가입된 멤버만 탈퇴할 수 있습니다.");
        }
        postRepo.releaseSeat(postId);

        // 벌크 UPDATE는 영속성 컨텍스트에 반영되지 않으므로 응답용 엔티티 값만 맞춰줌
        app.setStatus(RecruitApplication.Status.CANCELLED);

        return toResponse(app);
    }

    private List<ApplicationResponse> AcceptedApplications(Long postId) {
        return applicationRepo.findByPost_IdOrderByApplicationIdDesc(postId)
                .stream()
                .filter(a -> a.getStatus() == RecruitApplication.Status.ACCEPTED)
                .map(this::toResponse)
                .toList();
    }
}
