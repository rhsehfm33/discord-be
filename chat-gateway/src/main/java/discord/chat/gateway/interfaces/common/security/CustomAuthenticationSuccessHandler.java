package discord.chat.gateway.interfaces.common.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import discord.chat.gateway.infrastructure.user.User;
import discord.chat.gateway.infrastructure.user.UserMongoRepository;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserMongoRepository userMongoRepository;

    public CustomAuthenticationSuccessHandler(UserMongoRepository userMongoRepository) {
        this.userMongoRepository = userMongoRepository;
    }

    // Do nothing to prevent redirect
    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request, HttpServletResponse response, Authentication authentication
    ) throws IOException {
        User user = userMongoRepository.findByEmail(authentication.getName()).orElseThrow(() ->
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