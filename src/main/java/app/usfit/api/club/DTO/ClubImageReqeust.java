package app.usfit.api.club.DTO;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

@Data
public class ClubImageReqeust {
    private MultipartFile[] files;
    private String title;
    private String description;
}
