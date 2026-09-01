package discord.chat.api.infrastructure.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import discord.chat.common.infrastructure.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.PrivateKey;
import java.time.Instant;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    @Value("${jwt.key-id}")
    private String keyId;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.audience}")
    private String audience;

    @Value("${jwt.expiration-seconds}")
    private long expirationSeconds;

    private final JWKSet jwkSet;

    // issue an access token containing user info
    public String generateToken(User user) {
        try {
            // Retrieve the RSA private key
            RSAKey rsaKey = (RSAKey) jwkSet.getKeyByKeyId(keyId);
            PrivateKey privateKey = rsaKey.toPrivateKey();
            JWSSigner signer = new RSASSASigner(privateKey);

            // Create JWT claims
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(user.getId())
                .issuer(issuer)
                .audience(audience)
                .claim(JwtClaimNames.EMAIL, user.getEmail())
                .claim(JwtClaimNames.NICK_NAME, user.getNickName())
                .claim(JwtClaimNames.IMAGE_URL, user.getImageUrl())
                .expirationTime(Date.from(Instant.now().plusSeconds(expirationSeconds)))
                .issueTime(new Date())
                .build();

            // Build and sign JWT
            SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(keyId).build(),
                claimsSet
            );

            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate JWT", e);
        }
    }
}



