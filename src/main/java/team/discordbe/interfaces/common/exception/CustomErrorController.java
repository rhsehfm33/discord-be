package team.discordbe.interfaces.common.exception;

import java.util.Map;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping({"${server.error.path:${error.path:/error}}"})
public class CustomErrorController implements ErrorController {

    private final DefaultErrorAttributes defaultErrorAttributes;

    @RequestMapping
    public ResponseEntity<Map<String, Object>> handleError(HttpServletRequest request, HttpServletResponse response) {
        HttpStatus status = null;
        WebRequest webRequest = new ServletWebRequest(request, response);
        Throwable error = defaultErrorAttributes.getError(webRequest);
        if (error != null) {
            status = ExceptionHttpStatusMapper.getHttpStatus(error.getClass());
        }
        if (status == null) {
            status = getStatus(request);
        }

        Map<String, Object> errorAttributes = defaultErrorAttributes.getErrorAttributes(
            webRequest, ErrorAttributeOptions.of(ErrorAttributeOptions.Include.MESSAGE)
        );
        return new ResponseEntity<>(errorAttributes, status);
    }

    protected HttpStatus getStatus(HttpServletRequest request) {
        Integer statusCode = (Integer)request.getAttribute("jakarta.servlet.error.status_code");
        if (statusCode == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        } else {
            try {
                return HttpStatus.valueOf(statusCode);
            } catch (Exception var4) {
                return HttpStatus.INTERNAL_SERVER_ERROR;
            }
        }
    }
}