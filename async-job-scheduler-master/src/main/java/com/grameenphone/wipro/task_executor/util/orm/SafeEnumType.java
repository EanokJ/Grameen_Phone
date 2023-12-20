package com.grameenphone.wipro.task_executor.util.orm;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.EnumType;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SafeEnumType extends EnumType {
    private static Field superEnumClassField;

    static {
        try {
            superEnumClassField = EnumType.class.getDeclaredField("enumClass");
            superEnumClassField.setAccessible(true);
        } catch (NoSuchFieldException e) {
        }
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner) throws SQLException {
        try {
            return super.nullSafeGet(rs, names, session, owner);
        } catch (IllegalArgumentException e) {
            try {
                Class<? extends Enum> enumClass = (Class<? extends Enum>)superEnumClassField.get(this);
                DefaultEnumValue defaultEnumValue = enumClass.getAnnotation(DefaultEnumValue.class);
                if(defaultEnumValue == null) {
                    throw e;
                }
                return Enum.valueOf(enumClass, defaultEnumValue.value());
            } catch (IllegalAccessException ex) {
            }
            throw e;
        }
    }
}