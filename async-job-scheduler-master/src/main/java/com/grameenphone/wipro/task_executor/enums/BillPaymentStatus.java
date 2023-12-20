package com.grameenphone.wipro.task_executor.enums;

import com.grameenphone.wipro.task_executor.util.orm.DefaultEnumValue;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;

@DefaultEnumValue("Unknown")
public enum BillPaymentStatus {
    Success,
    Fail,
    Dispute,
    Rollback,
    Rollback_Fail("Rollback Fail"),
    Unknown; //To use as default in conversion by orm

    static Field nameField;

    private void initializeNameField() {
        try {
            nameField = Enum.class.getDeclaredField("name");
            AccessController.doPrivileged(
                    (PrivilegedAction<Object>) () -> {
                        nameField.setAccessible(true);
                        return null;
                    });
        } catch (Throwable g) {}
    }

    BillPaymentStatus() {}

    BillPaymentStatus(String name) {
        changeName(name);
    }

    private void changeName(String name) {
        try {
            if(nameField == null) {
                initializeNameField();
            }
            nameField.set(this, name);
        } catch (IllegalAccessException e) {}
    }
}