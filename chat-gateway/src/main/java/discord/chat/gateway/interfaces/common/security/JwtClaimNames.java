package discord.chat.gateway.interfaces.common.security;

/**
 * Claim names carried by the self-contained access token, shared by the issuer and every
 * resource server that has to rebuild the principal from it.
 */
public final class JwtClaimNames {
    public static final String EMAIL = "email";
    public static final String NICK_NAME = "nick_name";
    public static final String IMAGE_URL = "image_url";

    private JwtClaimNames() {
    }
}
