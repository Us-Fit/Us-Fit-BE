package app.usfit.api.RecruitPlayer.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.usfit.api.RecruitPlayer.dto.RecruitPlayerPostRequest;
import app.usfit.api.RecruitPlayer.entity.RecruitPlayerPost;
import app.usfit.api.RecruitPlayer.service.RecruitPlayerPostService;
import app.usfit.api.user.service.UserService;

@RestController
@RequestMapping("/api/recruits")
public class RecruitPlayerController {

    @Autowired
    private RecruitPlayerPostService service;
    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<RecruitPlayerPost> createPost(@RequestBody RecruitPlayerPostRequest req, Principal principal) {
        // principal에서 로그인된 유저 정보 가져오기 (Spring Security 사용 시)
        // User writer = userService.findByName(principal.getName()).orElse(null);

        // if (writer == null) {
        //     System.out.println("작성자 정보가 없습니다.");
        //     return ResponseEntity.status(401).build(); // Unauthorized
        // }
        //RecruitPlayerPost post = service.createPost(req, writer);

        RecruitPlayerPost post = service.createPost(req, null); // 임시로 null 전달
        return ResponseEntity.ok(post);
    }

    @GetMapping
    public List<RecruitPlayerPost> getAllActivePosts() {
        return service.getActivePosts();
    }
}