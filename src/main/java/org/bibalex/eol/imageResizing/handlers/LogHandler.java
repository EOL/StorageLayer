package org.bibalex.eol.imageResizing.handlers;

import java.util.logging.LogManager;
import org.apache.logging.log4j.Logger;

public class LogHandler {

    private static boolean initialized = false;

    public static void initializeHandler() {
        System.setProperty("log4j.configurationFile",
                ResourceHandler.getPropertyValue("log4jConfigurationFile"));
        initialized = true;
    }

    public static Logger getLogger(String loggerName) {
        if (!initialized) {
            System.err.println("LogHandler not initialized !");
        }
        return org.apache.logging.log4j.LogManager.getLogger(loggerName);
    }

}