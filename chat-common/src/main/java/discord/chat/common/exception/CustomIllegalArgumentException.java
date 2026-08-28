package discord.chat.common.exception;

public class CustomIllegalArgumentException extends CustomException {
    public CustomIllegalArgumentException(String errorCode, String message) {
        super(errorCode, message);
    }
}
