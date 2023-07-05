package dev.roshin.openliberty.repl.controllers.jmx.rest;

public class JMXConstants {
    public static final String SERVER_INFO_MBEAN_OBJECT_QUERY = "WebSphere:feature=kernel,name=ServerInfo";
    public static final String APPLICATION_MBEAN_OBJECT_QUERY = "WebSphere:service=com.ibm.websphere.application.ApplicationMBean,name=*";
    public static final String APPLICATION_MBEAN_RESTART_OPERATION = "restart";
    public static final String FRAMEWORK_MBEAN_OBJECT_QUERY = "osgi.core:type=framework,version=*,framework=org.eclipse.osgi,uuid=*";
    public static final String FRAMEWORK_MBEAN_SHUTDOWN_OPERATION = "shutdownFramework";
}
