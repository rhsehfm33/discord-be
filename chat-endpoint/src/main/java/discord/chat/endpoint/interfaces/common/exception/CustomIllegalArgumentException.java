package discord.chat.endpoint.interfaces.common.exception;

public class CustomIllegalArgumentException extends CustomException {
    public CustomIllegalArgumentException(String errorCode, String message) {
        super(errorCode, message);
    }
}
