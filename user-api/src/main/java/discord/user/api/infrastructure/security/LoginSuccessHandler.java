package discord.user.api.infrastructure.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import discord.chat.common.infrastructure.user.User;
import discord.chat.common.infrastructure.user.UserMongoRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AccessTokenCookieFactory accessTokenCookieFactory;
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
        response.addCookie(accessTokenCookieFactory.create(token, 3600));

        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().print("{\"status\":\"success\"}");
        response.getWriter().flush();
    }
}


