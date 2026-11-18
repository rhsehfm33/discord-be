package discord.user.api.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;

@Component
public class AccessTokenCookieFactory {
    @Value("${domain-name}")
    private String domainName;

    public Cookie create(String token, int maxAge) {
        Cookie cookie = new Cookie("access_token", token);
        cookie.setDomain(domainName);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(maxAge);
        return cookie;
    }
}
