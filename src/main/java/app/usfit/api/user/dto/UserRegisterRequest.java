package app.usfit.api.user.dto;

import lombok.Data;

@Data
public class UserRegisterRequest {
    private String email;
    private String nickname;
    private String password;
    
    // 생성자
    public UserRegisterRequest() {}
    
    public UserRegisterRequest(String email, String nickname, String password) {
        this.email = email;
        this.nickname = nickname;
        this.password = password;
    }
}