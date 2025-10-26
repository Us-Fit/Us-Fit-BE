package app.usfit.api.RecruitPlayer.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import app.usfit.api.RecruitPlayer.dto.RecruitPlayerPostRequest;
import app.usfit.api.RecruitPlayer.entity.RecruitPlayerPost;
import app.usfit.api.RecruitPlayer.repository.RecruitPlayerPostRepository;
import app.usfit.api.user.entity.User;

@Service
public class RecruitPlayerPostService {
    @Autowired
    private RecruitPlayerPostRepository repository;

    public RecruitPlayerPost createPost(RecruitPlayerPostRequest req, User writer) {
        RecruitPlayerPost post = new RecruitPlayerPost();
        post.setTitle(req.getTitle());
        post.setDescription(req.getDescription());
        post.setSportType(req.getSportType());
        post.setLocation(req.getLocation());
        post.setMaxMemberCount(req.getMaxMemberCount());
        post.setCurrMemeberCount(req.getCurrMemeberCount());
        post.setActive(true);
        //post.setWriter(writer);
        
        return repository.save(post);
    }

    public List<RecruitPlayerPost> getActivePosts() {
        return repository.findByIsActiveTrue();
    }
}