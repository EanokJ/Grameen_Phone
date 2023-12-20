package com.grameenphone.wipro.task_executor.util;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

public class LookupUtil {
    public static File lookupConfFile(String configFile) throws URISyntaxException {
        System.out.println("Started lookup " + configFile);
        File config = getFromCatalina(configFile);
        if(config.exists()) {
            return config;
        }
        config = getFromCurrentDir(configFile);
        if(config.exists()) {
            return config;
        }
        config = getFromClassLocationRecursive(configFile);
        if(config.exists()) {
            return config;
        }
        config = getFromUserRoot(configFile);
        if(config.exists()) {
            return config;
        }
        return null;
    }

    private static File getFromCatalina(String configFile) {
        String configLocation = System.getProperty("catalina.home") + File.separatorChar + "conf";
        return new File(configLocation, configFile);
    }

    private static File getFromUserRoot(String configFile) {
        String configLocation = System.getProperty("user.home") + File.separatorChar + ".fmfs" + File.separatorChar + "conf";
        return new File(configLocation, configFile);
    }

    private static File getFromCurrentDir(String configFile) {
        System.out.println("Looking up in ${current}/conf = " + new File("").getAbsolutePath());
        return new File("conf" + File.separatorChar + configFile);
    }

    private static File getFromClassLocationRecursive(String configFile) throws URISyntaxException {
        URL url = LookupUtil.class.getResource("LookupUtil.class");
        URI uri;
        if(!url.getProtocol().equals("file")) {
            uri = new URI(url.getFile());
        } else {
            uri = url.toURI();
        }
        String classRoot = Paths.get(uri).toFile().getAbsolutePath();
        System.out.println("Looking up in ${class root}/conf = " + classRoot);
        int webinfIndex = classRoot.indexOf("WEB-INF");
        classRoot = webinfIndex > 0 ? classRoot.substring(0, webinfIndex) : classRoot; //if webinf not found then running from test codes
        System.out.println("Looking up in ${class root upto web-inf}/conf = " + classRoot);
        File configDir = new File(classRoot);
        File config = new File(configDir, "conf" + File.separatorChar + configFile);
        while(configDir != null && !config.exists()) {
            configDir = configDir.getParentFile();
            System.out.println("Looking up in ${class root upto web-inf}/conf = " + configDir.getAbsolutePath());
            config = new File(configDir, "conf" + File.separatorChar + configFile);
        }
        return config;
    }
}