package team.discordbe.global.exception;

public class CustomResourceConflictException extends CustomException {
    public CustomResourceConflictException(String errorCode, String message) {
        super(errorCode, message);
    }
}
