package com.grameenphone.wipro.task_executor.test;

import com.grameenphone.wipro.task_executor.Main;
import com.grameenphone.wipro.task_executor.util.PropertyUtil;
import org.testng.annotations.AfterSuite;

import java.net.URISyntaxException;

public class TestBaseContext {
    static {
        try {
            PropertyUtil.cacheProperties(PropertyUtil.DEFAULT_TEST_PROPERTY_FILE_NAME);
        } catch (URISyntaxException e) {
            System.out.println("Configuration file could not be read");
        }
        Main.initApplicationContexts();
        System.setProperty("http.mock.enabled", "true");
    }
}