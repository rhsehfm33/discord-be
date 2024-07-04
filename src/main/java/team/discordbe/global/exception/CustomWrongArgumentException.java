package team.discordbe.global.exception;

public class CustomWrongArgumentException extends CustomException {
    public CustomWrongArgumentException(String errorCode, String message) {
        super(errorCode, message);
    }
}
