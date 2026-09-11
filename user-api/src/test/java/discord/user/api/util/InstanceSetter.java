package discord.user.api.util;

import java.lang.reflect.Field;

public class InstanceSetter {

    public static void setField(Object targetObject, String fieldName, Object fieldValue)
        throws NoSuchFieldException, IllegalAccessException {
        Class<?> targetClass = targetObject.getClass();
        Field targetField = targetClass.getDeclaredField(fieldName);
        targetField.setAccessible(true);
        targetField.set(targetObject, fieldValue);
    }

}
