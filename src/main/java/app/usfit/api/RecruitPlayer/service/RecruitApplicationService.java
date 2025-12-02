package app.usfit.api.RecruitPlayer.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import app.usfit.api.RecruitPlayer.dto.ApplicationApplyRequest;
import app.usfit.api.RecruitPlayer.dto.ApplicationResponse;
import app.usfit.api.RecruitPlayer.dto.ApplicationStatusRequest;
import app.usfit.api.RecruitPlayer.entity.RecruitApplication;
import app.usfit.api.RecruitPlayer.entity.RecruitPlayerPost;
import app.usfit.api.RecruitPlayer.repository.RecruitApplicationRepository;
import app.usfit.api.RecruitPlayer.repository.RecruitPlayerPostRepository;
import app.usfit.api.user.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class RecruitApplicationService {
    private final RecruitApplicationRepository applicationRepo;
    private final RecruitPlayerPostRepository postRepo;

    // persistenceContext -> EntityManager를 주입받기 위한 어노테이션 (우체국 직원 느낌.)
    // EntityManager -> JPA의 핵심 인터페이스로, 엔티티의 생명주기 관리, 쿼리 실행 등을 담당 (JPA에서 DB와 소통)
    @PersistenceContext 
    private EntityManager entityManager;

    public RecruitApplicationService(RecruitApplicationRepository recruitApplicationRepository,
            RecruitPlayerPostRepository recruitPlayerPostService) {
        this.applicationRepo = recruitApplicationRepository;
        this.postRepo = recruitPlayerPostService;
    }

    @Transactional
    public ApplicationResponse apply(Long postId, Long applicantId, ApplicationApplyRequest req) {
        RecruitPlayerPost post = postRepo.findById(postId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 모집글"));
        
        if (post.getWriter().getId().equals(applicantId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "본인의 모집글에는 지원할 수 없습니다.");
        }

        if (applicationRepo.existsByPost_IdAndApplicant_Id(postId, applicantId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 지원한 모집글입니다.");
        }

        RecruitApplication app = new RecruitApplication();
        app.setPost(post);
        app.setApplicant(entityManager.getReference(User.class, applicantId));
        app.setIntroduction(req == null ? null : req.introduction());

        RecruitApplication saved = applicationRepo.save(app);
        return toResponse(saved);
    }

    // 엔티티 -> 응답 DTO 매핑 헬퍼
    private ApplicationResponse toResponse(RecruitApplication a) {
        return new ApplicationResponse(
            a.getApplicationId(),
            a.getPost().getId(),
            a.getApplicant().getId(),
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
        // 상태 변경
        app.setStatus(req.status());
        applicationRepo.save(app);
        return toResponse(app);
    }


    // 내가 신청한 모집글 조회
    @Transactional
    public List<ApplicationResponse> listMyApplications(Long userId) {
        return applicationRepo.findByApplicant_IdOrderByApplicationIdDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    // 멤버용: ACCEPTED만
    public List<ApplicationResponse> listForPostMember(Long postId, Long requesterId) {
        RecruitPlayerPost post = postRepo.findById(postId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 모집글"));
        
        List<ApplicationResponse> response = applicationRepo.findByPost_IdOrderByApplicationIdDesc(postId)
                .stream().filter(a -> a.getStatus() == RecruitApplication.Status.ACCEPTED).map(this::toResponse).toList();

        // 작성자 또는 ACCEPTED된 멤버인지 확인
        boolean requesterIsAccepted = response.stream()
                .anyMatch(a -> a.applicantId() != null && a.applicantId().equals(requesterId)) || post.getWriter().getId().equals(requesterId);

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

        // 현재 상태가 ACCEPTED(가입된 상태)인지 확인
        if (app.getStatus() != RecruitApplication.Status.ACCEPTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "가입된 멤버만 탈퇴할 수 있습니다.");
        }
        
        // 상태를 CANCLED로 변경
        app.setStatus(RecruitApplication.Status.CANCELLED);
        applicationRepo.save(app);

        return toResponse(app);
    }
    
}
