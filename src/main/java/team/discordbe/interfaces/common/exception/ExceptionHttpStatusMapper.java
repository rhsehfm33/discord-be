package team.discordbe.interfaces.common.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;

public class ExceptionHttpStatusMapper {
    private static final Map<Class<? extends Throwable>, HttpStatus> EXCEPTION_STATUS_MAP = new HashMap<>();

    static {
        EXCEPTION_STATUS_MAP.put(CustomEntityNotFoundException.class, HttpStatus.NOT_FOUND);
        EXCEPTION_STATUS_MAP.put(CustomIllegalArgumentException.class, HttpStatus.BAD_REQUEST);
        EXCEPTION_STATUS_MAP.put(CustomResourceConflictException.class, HttpStatus.CONFLICT);
        EXCEPTION_STATUS_MAP.put(CustomAuthorizationError.class, HttpStatus.FORBIDDEN);
    }

    static HttpStatus getHttpStatus(final Class<? extends Throwable> exceptionClass) {
        return EXCEPTION_STATUS_MAP.get(exceptionClass);
    }
}
