package com.grameenphone.wipro.task_executor.util;

import java.util.Calendar;

public class BillNoUtil {
    public static String generateUniqueBillNo(String billType) {
        StringBuilder sb = new StringBuilder();
        sb.append(billType);
        sb.append(String.format("%02d", Calendar.getInstance().get(Calendar.DAY_OF_MONTH)));
        sb.append(("" + System.currentTimeMillis()).substring(3));
        sb.append(String.format("%03d", System.nanoTime() % 1000));
        return sb.toString();
    }
}
