package app.usfit.api.user;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserRepository repo;
    public UserController(UserRepository repo){
        this.repo = repo;
    }

    @GetMapping
    public List<User> all(){
        return repo.findAll();
    }

    @PostMapping
    public User create(@RequestParam String name){
        return repo.save(User.builder().name(name).build());
    }
}
