package dev.roshin.openliberty.repl.jmx.rest;

public class MBeanInfo {
    private String objectName;
    private String className;
    private String URL;


    // Getter Methods

    public String getObjectName() {
        return objectName;
    }

    public String getClassName() {
        return className;
    }

    public String getURL() {
        return URL;
    }

    // Setter Methods

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }
}