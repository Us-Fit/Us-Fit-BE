package app.usfit.api.RecruitPlayer.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.usfit.api.RecruitPlayer.dto.RecruitPlayerPostRequest;
import app.usfit.api.RecruitPlayer.entity.RecruitPlayerPost;
import app.usfit.api.RecruitPlayer.repository.RecruitPlayerPostRepository;
import app.usfit.api.sport.entity.Sport;
import app.usfit.api.user.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class RecruitPlayerPostService {
    @Autowired
    private final RecruitPlayerPostRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    public RecruitPlayerPostService(RecruitPlayerPostRepository repository) {
        this.repository = repository;
    }
    
    @Transactional
    public RecruitPlayerPost createPost(RecruitPlayerPostRequest req, Long writerId) {
        // 작성자 User 엔티티 참조 가져오기
        User writer = entityManager.getReference(User.class, writerId);
        if (writer == null) {
            throw new IllegalArgumentException("작성자 User를 찾을 수 없습니다.");
        }
        // Sport 엔티티 참조 가져오기
        Sport sport = entityManager.getReference(Sport.class, req.getSportTypeId());
        if (sport == null) {
            throw new IllegalArgumentException("스포츠 유형을 찾을 수 없습니다.");
        }

        RecruitPlayerPost post = new RecruitPlayerPost();
        post.setWriter(writer);
        post.setSport(sport);
        post.setTitle(req.getTitle());
        post.setDescription(req.getDescription());
        post.setLocation(req.getLocation());
        post.setRecruitDeadline(req.getRecruitDeadline());
        post.setActivityStartTime(req.getActivityStartTime());
        post.setActivityDurationMinutes(req.getActivityDurationMinutes());
        post.setMaxMember(req.getMaxMember());
        post.setIsActive(true);

        return repository.save(post);
    }

    public List<RecruitPlayerPost> getActivePosts() {
        return repository.findByIsActiveTrue();
    }

    public List<RecruitPlayerPost> getMyPosts(Long userId, boolean activeOnly) {
        if (activeOnly) {
            return repository.findByWriter_IdAndIsActiveTrueOrderByIdDesc(userId);
        } else {
            return repository.findByWriter_IdOrderByIdDesc(userId);
        }
    }
}