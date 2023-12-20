package com.grameenphone.wipro.task_executor.util;

public class NumberUtil {
    public static String getOrdinal(int number) {
        switch(number % 10) {
            case 1:
                return "st";
            case 2:
                return "nd";
            case 3:
                return "rd";
            default:
                return "th";
        }
    }
}
