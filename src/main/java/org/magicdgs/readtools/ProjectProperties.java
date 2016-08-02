/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Daniel Gómez-Sánchez
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 */
package org.magicdgs.readtools;

import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Properties;

/**
 * Static methods for retrieve Project properties as version, program name and build
 *
 * @author Daniel Gómez-Sánchez
 */
public class ProjectProperties {

    private static final String PROPERTIES_FILE = "/version.prop";

    /**
     * The default properties
     */
    private static final Hashtable<String, String> DEFAULT_VERSION_VALUES =
            new Hashtable<String, String>() {{
                put("version", "UNKNOWN");
                put("name", "Program");
                put("build", "develop"); // the build will be computed except it is in develop
                put("timestamp", "unknown");
                put("contact_person", "DGS");
                put("contact_email", "");
            }};

    private static String name = null;

    private static String version = null;

    private static String build = null;

    private static String timestamp = null;

    private static String contactPerson = null;

    private static String contactEmail = null;

    /**
     * Get the name of the program
     *
     * @return the name of the program
     */
    public static String getName() {
        if (name == null) {
            setValue("name");
        }
        return name;
    }

    /**
     * Get the version for the program
     *
     * @return the version
     */
    public static String getVersion() {
        if (version == null) {
            setValue("version");
        }
        return version;
    }

    /**
     * Get the build for this project
     *
     * @return the build String
     */
    public static String getBuild() {
        if (build == null) {
            setValue("build");
        }
        return build;
    }

    /**
     * Get the compilation time
     *
     * @return the timestamp
     */
    public static String getTimestamp() {
        if (timestamp == null) {
            setValue("timestamp");
        }
        return timestamp;
    }

    /**
     * Get the contact person
     *
     * @return the contact person
     */
    public static String getContactPerson() {
        if (contactPerson == null) {
            setValue("contact_person");
        }
        return contactPerson;
    }

    /**
     * Get the contact email
     *
     * @return the contact email
     */
    public static String getContactEmail() {
        if (contactEmail == null) {
            setValue("contact_email");
        }
        return contactEmail;
    }

    /**
     * Get the formatted version in the format v.${version}.r_${build}
     *
     * @return the formatted version
     */
    public static String getFormattedVersion() {
        if (version == null || build == null) {
            getAllPropertiesForProgramHeader();
        }
        return String.format("%s.r_%s", version, build);
    }

    /**
     * Get the formatted name with version like Name v.${version}.r_${build}
     *
     * @return the formatted name with version
     */
    public static String getFormattedNameWithVersion() {
        if (version == null || build == null || name == null) {
            getAllPropertiesForProgramHeader();
        }
        return String.format("%s v.%s", name, getFormattedVersion());
    }

    /**
     * Get the full contact (Name + email)
     *
     * @return the full contact
     */
    public static String getContact() {
        if (contactPerson == null || contactEmail == null) {
            getAllPropertiesForProgramHeader();
        }
        return String.format("%s (%s)", contactPerson, contactEmail);
    }

    /**
     * Get the operating system where the program is running as ${os.name} ${os.version}
     * (${os.arch})
     *
     * @return the formatted string
     */
    public static String getOperatingSystem() {
        return String.format("%s %s (%s)", System.getProperty("os.name"),
                System.getProperty("os.version"),
                System.getProperty("os.arch"));
    }

    /**
     * Get a value from the property file
     *
     * @param tag the tag in the property file
     *
     * @return the value
     */
    private static String getFromProperties(String tag) {
        InputStream stream = ProjectProperties.class.getResourceAsStream(PROPERTIES_FILE);
        if (stream == null) {
            return DEFAULT_VERSION_VALUES.get(tag);
        }
        Properties props = new Properties();
        try {
            props.load(stream);
            stream.close();
            String prop = (String) props.get(tag);
            return (prop == null) ? DEFAULT_VERSION_VALUES.get(tag) : prop;
        } catch (IOException e) {
            return DEFAULT_VERSION_VALUES.get(tag);
        }
    }

    /**
     * Get all the properties at the same time from file
     */
    private static void getAllPropertiesForProgramHeader() {
        InputStream stream = ProjectProperties.class.getResourceAsStream(PROPERTIES_FILE);
        if (stream == null) {
            setDefaults();
            return;
        }
        Properties props = new Properties();
        try {
            props.load(stream);
            stream.close();
            for (String tag : DEFAULT_VERSION_VALUES.keySet()) {
                String prop = (String) props.get(tag);
                String val = (isAbsent(prop)) ? DEFAULT_VERSION_VALUES.get(tag) : prop;
                setValue(tag, val);
            }
        } catch (IOException e) {
            setDefaults();
        }
    }

    /**
     * Check if the property value is absent (null or start with $)
     *
     * @param value the value to test
     *
     * @return <code>true</code> if it is absent; <code>false</code> otherwise
     */
    private static boolean isAbsent(String value) {
        return value == null || value.contains("$");
    }

    /**
     * Set a tag value
     *
     * @param tag the tag to set
     */
    private static void setValue(String tag) {
        String val = getFromProperties(tag);
        if (isAbsent(val)) {
            setDefault(tag);
        } else {
            setValue(tag, val);
        }
    }

    /**
     * Set a tag with a value
     *
     * @param tag the tag to set
     * @param val the value
     */
    private static void setValue(String tag, String val) {
        switch (tag) {
            case "name":
                name = val;
                break;
            case "build":
                build = val;
                break;
            case "timestamp":
                timestamp = val;
                break;
            case "version":
                version = val;
                break;
            case "contact_person":
                contactPerson = val;
                break;
            case "contact_email":
                contactEmail = val;
                break;
            default:
                throw new IllegalArgumentException("Property " + tag + " not found");
        }
    }

    /**
     * Set the default value for a tag
     *
     * @param tag the tag to set
     */
    private static void setDefault(String tag) {
        String val = DEFAULT_VERSION_VALUES.get(tag);
        setValue(tag, val);
    }

    /**
     * Set all values for all the tags
     */
    private static void setDefaults() {
        DEFAULT_VERSION_VALUES.keySet()
                .forEach(org.magicdgs.readtools.ProjectProperties::setDefault);
    }
}
