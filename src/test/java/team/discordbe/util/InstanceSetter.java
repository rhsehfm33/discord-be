package team.discordbe.util;

import java.lang.reflect.Field;

public class InstanceSetter {

    public static void setField(Object obj, String fieldName, Object value)
        throws NoSuchFieldException, IllegalAccessException {
        Class<?> clazz = obj.getClass();
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

}
