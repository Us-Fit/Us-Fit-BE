package app.usfit.api.user.dto;

import lombok.Data;

@Data
public class UserRegisterRequest {
    private String email;
    private String password;
    
    // 생성자
    // JackSon이 JSON 객체로 바인딩 할때 기본 생성자 호출 후 setter로 값을 주입한다 함.
    public UserRegisterRequest() {}
    
    public UserRegisterRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }
}