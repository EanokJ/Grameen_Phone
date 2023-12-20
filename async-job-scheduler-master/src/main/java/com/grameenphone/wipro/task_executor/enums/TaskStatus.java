package com.grameenphone.wipro.task_executor.enums;

import com.grameenphone.wipro.task_executor.util.orm.DefaultEnumValue;

@DefaultEnumValue("Unknown")
public enum TaskStatus {
    Pending,
    Processing,
    Completed,
    Unknown //To use as default in conversion by orm
}
