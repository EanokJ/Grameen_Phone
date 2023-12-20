package com.grameenphone.wipro.task_executor.util;

import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import java.io.File;
import java.io.FileInputStream;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Properties;

public class PropertyUtil {
    public final static String DEFAULT_PROPERTY_FILE_NAME = "scheduler.properties";
    public final static String DEFAULT_TEST_PROPERTY_FILE_NAME = "test.scheduler.properties";
    public final static String CONFIG_SOURCE_ID = "external_config_source";

    private static Properties properties = new Properties();

    public static boolean cacheProperties(String propertyFile) throws URISyntaxException {
        File configFile = LookupUtil.lookupConfFile(propertyFile);
        try (FileInputStream inputStream = new FileInputStream(configFile)) {
            properties.load(inputStream);
        } catch (Throwable j) {
            return false;
        }
        return true;
    }

    public static PropertySource getPropertySource() {
        return new MapPropertySource(CONFIG_SOURCE_ID, new LinkedHashMap(properties));
    }

    public static String getProperty(String value) {
        return properties.getProperty(value);
    }

    public static String getProperty(String value, String defaultValue) {
        String _value = properties.getProperty(value);
        if (_value == null) {
            _value = defaultValue;
        }
        return _value;
    }
}