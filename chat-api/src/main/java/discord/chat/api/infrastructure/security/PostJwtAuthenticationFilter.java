package discord.chat.api.infrastructure.security;

import discord.chat.common.infrastructure.user.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class PostJwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
            && authentication.isAuthenticated()
            && authentication.getPrincipal() instanceof Jwt jwt) {
            Authentication newAuth = new UsernamePasswordAuthenticationToken(
                toUser(jwt),
                authentication.getCredentials(),
                authentication.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(newAuth);
        }

        filterChain.doFilter(request, response);
    }

    private User toUser(Jwt jwt) {
        return new User(
            jwt.getSubject(),
            jwt.getClaimAsString(JwtClaimNames.NICK_NAME),
            jwt.getClaimAsString(JwtClaimNames.EMAIL),
            null,   // password should not be exposed
            jwt.getClaimAsString(JwtClaimNames.IMAGE_URL)
        );
    }
}



