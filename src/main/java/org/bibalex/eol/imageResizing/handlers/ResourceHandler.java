package org.bibalex.eol.imageResizing.handlers;
import org.bibalex.eol.imageResizing.utils.PropertyLoader;

import java.util.Properties;


public class ResourceHandler {

    private static Properties props = new Properties();
    public static void initialize(String propFileName){
        props = PropertyLoader.loadProperties(propFileName);
    }

    /**
     * This method used to retrieve the value of the given property name.
     * @param propertyName
     * @return the value of the property
     */
    public static String getPropertyValue(String propertyName) {
        String propValue = props.getProperty(propertyName);
        return propValue;
    }
}
