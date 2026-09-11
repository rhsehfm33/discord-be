package discord.chat.api.support;

import java.lang.reflect.Field;

public class InstanceSetter {

    // Sets a private field on a test object.
    public static void setField(Object targetObject, String fieldName, Object fieldValue)
        throws NoSuchFieldException, IllegalAccessException {
        Class<?> targetClass = targetObject.getClass();
        Field targetField = targetClass.getDeclaredField(fieldName);
        targetField.setAccessible(true);
        targetField.set(targetObject, fieldValue);
    }

}
