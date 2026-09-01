package discord.chat.api.infrastructure.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import discord.chat.common.infrastructure.user.User;
import discord.chat.common.infrastructure.user.UserMongoRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    @Value("${domain-name}")
    private String domainName;

    private final UserMongoRepository userMongoRepository;
    private final JwtUtil jwtUtil;

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

        // Set access token in a cookie
        String token = jwtUtil.generateToken(user);
        Cookie accessTokenCookie = new Cookie("access_token", token);
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setDomain(domainName);
        accessTokenCookie.setMaxAge(3600); // 1 hour expiration
        response.addCookie(accessTokenCookie);

        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().print("{\"status\":\"success\"}");
        response.getWriter().flush();
    }
}


