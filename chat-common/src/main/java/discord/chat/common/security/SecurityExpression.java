package discord.chat.common.security;

public final class SecurityExpression {
    public static final String INTERNAL_ONLY = "hasAuthority('SCOPE_internal')";

    private SecurityExpression() {
    }
}
