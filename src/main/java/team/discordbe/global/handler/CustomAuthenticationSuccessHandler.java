package team.discordbe.global.handler;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    // Do nothing to prevent redirect
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws
        IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().print("{\"status\":\"success\"}");
        response.getWriter().flush();
    }
}