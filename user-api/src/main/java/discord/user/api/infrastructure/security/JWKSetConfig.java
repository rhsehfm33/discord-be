package discord.user.api.infrastructure.security;

import java.io.IOException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;

@Configuration
public class JWKSetConfig {
    private final String keyId;
    private final Resource privateKeyResource;
    private final Resource publicKeyResource;

    public JWKSetConfig(
        @Value("${jwt.key-id}") String keyId,
        @Value("${jwt.private-key-location}") Resource privateKeyResource,
        @Value("${jwt.public-key-location}") Resource publicKeyResource
    ) {
        this.keyId = keyId;
        this.privateKeyResource = privateKeyResource;
        this.publicKeyResource = publicKeyResource;
    }

    @Bean
    public JWKSet jwkSet() {
        RSAPrivateKey privateKey = readPrivateKey();
        RSAPublicKey publicKey = readPublicKey();
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID(keyId)
            .build();

        return new JWKSet(rsaKey);
    }

    private RSAPrivateKey readPrivateKey() {
        try (var inputStream = privateKeyResource.getInputStream()) {
            return RsaKeyConverters.pkcs8().convert(inputStream);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException(
                "Failed to read JWT private key from " + privateKeyResource.getDescription(),
                exception
            );
        }
    }

    private RSAPublicKey readPublicKey() {
        try (var inputStream = publicKeyResource.getInputStream()) {
            return RsaKeyConverters.x509().convert(inputStream);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException(
                "Failed to read JWT public key from " + publicKeyResource.getDescription(),
                exception
            );
        }
    }
}


