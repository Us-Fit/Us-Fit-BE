package app.usfit.api.Utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ImageConvertUtils {
    @Value("${cloud.aws.s3.bucket:}")
    private String bucket;

    public String keyToUrl(String imageKey) {
        if (imageKey == null || imageKey.isBlank()) return imageKey;

        if (!StringUtils.hasText(bucket)) {
            return imageKey;
        }
        return "https://" + bucket + ".s3.amazonaws.com/" + imageKey;
    }
}
