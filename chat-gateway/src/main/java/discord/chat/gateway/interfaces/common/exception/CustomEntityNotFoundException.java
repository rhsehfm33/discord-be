package discord.chat.gateway.interfaces.common.exception;

public class CustomEntityNotFoundException extends CustomException {
    public CustomEntityNotFoundException(String errorCode, String message) {
        super(errorCode, message);
    }
}
