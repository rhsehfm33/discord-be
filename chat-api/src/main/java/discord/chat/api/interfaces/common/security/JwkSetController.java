package discord.chat.api.interfaces.common.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nimbusds.jose.jwk.JWKSet;

@RestController
public class JwkSetController {

    private final JWKSet jwkSet;

    @Autowired
    public JwkSetController(JWKSet jwkSet) {
        this.jwkSet = jwkSet;
    }

    @GetMapping("/oauth2/jwks")
    public String getJwkSet() {
        return jwkSet.toJSONObject().toString();
    }
}
