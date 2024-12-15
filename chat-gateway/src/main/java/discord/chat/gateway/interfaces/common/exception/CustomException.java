package discord.chat.gateway.interfaces.common.exception;

import lombok.Getter;

@Getter
public class CustomException extends Exception {
    private final String errorCode;

    public CustomException(String errorCode, String message) {
        super(message == null || message.isBlank() ? "NONE" : message);
        this.errorCode = errorCode == null || errorCode.isBlank() ? "NONE" : errorCode;
    }
}
