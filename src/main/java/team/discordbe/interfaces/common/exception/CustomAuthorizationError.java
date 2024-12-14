package team.discordbe.interfaces.common.exception;

public class CustomAuthorizationError extends CustomException {
    public CustomAuthorizationError(String errorCode, String message) {
        super(errorCode, message);
    }
}
