package team.discordbe.global.handler;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import team.discordbe.domain.user.model.User;
import team.discordbe.domain.user.repository.UserRepository;

public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;

    public CustomAuthenticationSuccessHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Do nothing to prevent redirect
    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request, HttpServletResponse response, Authentication authentication
    ) throws IOException {
        User user = userRepository.findByEmail(authentication.getName()).orElseThrow(() ->
            new EntityNotFoundException("User not found with email : " + authentication.getName()));

        Authentication newAuth = new UsernamePasswordAuthenticationToken(
            user,
            authentication.getCredentials(),
            authentication.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(newAuth);

        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().print("{\"status\":\"success\"}");
        response.getWriter().flush();
    }
}