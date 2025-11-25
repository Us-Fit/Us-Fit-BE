package app.usfit.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * Swagger UI가 multipart/form-data 내부의 JSON 파트에 Content-Type을 명시하지 않아
     * application/octet-stream으로 전달되는 경우가 있어 Jackson이 변환하지 못했다.
     * Jackson 컨버터가 octet-stream도 처리하도록 허용해 예외 없이 파싱되게 한다.
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.stream()
                .filter(MappingJackson2HttpMessageConverter.class::isInstance)
                .map(MappingJackson2HttpMessageConverter.class::cast)
                .forEach(converter -> {
                    List<MediaType> mediaTypes = new ArrayList<>(converter.getSupportedMediaTypes());
                    if (!mediaTypes.contains(MediaType.APPLICATION_OCTET_STREAM)) {
                        mediaTypes.add(MediaType.APPLICATION_OCTET_STREAM);
                        converter.setSupportedMediaTypes(mediaTypes);
                    }
                });
    }
}

