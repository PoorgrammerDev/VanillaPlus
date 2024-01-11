package io.github.poorgrammerdev.nightmare.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class ServerProperties {

    /**
     * Gets a field from a properties file
     * @param propertiesFile file to read from
     * @param setting setting/field to read from the properties file (e.g. level-name)
     * @return value of field
     * @throws IOException could not access properties file
     */
    public static String getSetting(final File propertiesFile, final String setting) throws IOException {
        final Properties properties = new Properties();
        final FileInputStream inputStream = new FileInputStream(propertiesFile);

        properties.load(inputStream);
        return properties.getProperty(setting);
    }

    /**
     * Set a field in a properties file
     * @param propertiesFile file to write to
     * @param setting setting/field to edit
     * @param value new value of field
     * @throws IOException could not access properties file
     */
    public static void setSetting(final File propertiesFile, final String setting, final String value) throws IOException {
        final Properties properties = new Properties();
        final FileInputStream inputStream = new FileInputStream(propertiesFile);

        properties.load(inputStream);
        properties.setProperty(setting, value);
        properties.store(new FileOutputStream(propertiesFile), null);
    }
}