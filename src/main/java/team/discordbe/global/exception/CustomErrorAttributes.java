package team.discordbe.global.exception;

import java.util.Map;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

@Component
public class CustomErrorAttributes extends DefaultErrorAttributes {
    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        options = options.excluding(ErrorAttributeOptions.Include.STACK_TRACE);
        return super.getErrorAttributes(webRequest, options);
    }

    @Override
    public Throwable getError(WebRequest webRequest) {
        return super.getError(webRequest);
    }
}