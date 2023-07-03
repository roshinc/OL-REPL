package dev.roshin.openliberty.repl.jmx;

import dev.roshin.openliberty.repl.jmx.domain.ApplicationStatus;
import dev.roshin.openliberty.repl.jmx.domain.ServerInfo;
import dev.roshin.openliberty.repl.jmx.rest.JmxClient;
import dev.roshin.openliberty.repl.jmx.rest.domain.attributes.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class JMXServerManagerImpl implements JMXServerManager {

    private final URL baseURL;
    private final String username;
    private final String password;
    private final int timeout;
    private final int retries;
    private JmxClient jmxClient;

    private final Logger logger;

    public JMXServerManagerImpl(URL baseURL, String username, String password) throws Exception {
        this(baseURL, username, password, 5000, 3);
    }

    public JMXServerManagerImpl(URL baseURL, String username, String password, int timeout, int retries) throws Exception {
        this.baseURL = baseURL;
        this.username = username;
        this.password = password;
        this.timeout = timeout;
        this.retries = retries;

        this.jmxClient = new JmxClient(baseURL, username, password, timeout, retries);
        this.logger = LoggerFactory.getLogger(getClass());
    }

    @Override
    public boolean isConnectable() throws Exception {
        logger.debug("Trying to connect to JMX Server");
        // Try to get all MBeans, if we get an ConnectException, return false
        try {
            jmxClient.getListOfAllMBeans();
            logger.debug("Successfully connected to JMX Server");
            return true;
        } catch (Exception e) {
            logger.error("Failed to connect to JMX Server", e);
            return false;
        }
    }

    @Override
    public ServerInfo getServerInfo() {
        logger.debug("Getting Server Info");
        try {
            List<Attribute> serverInfo = jmxClient.getServerInfo();
            ServerInfo serverInfoObj = new ServerInfo();
            //Loop through all the attributes and set the values
            for (Attribute attribute : serverInfo) {
                switch (attribute.getName()) {
                    case "Name" -> serverInfoObj.setServerName(attribute.getValue().getValue());
                    case "DefaultHostname" -> serverInfoObj.setDefaultHostname(attribute.getValue().getValue());
                    case "UserDirectory" -> serverInfoObj.setUserDirectory(Paths.get(attribute.getValue().getValue()));
                    case "InstallDirectory" ->
                            serverInfoObj.setInstallDirectory(Paths.get(attribute.getValue().getValue()));
                    case "LibertyVersion" -> serverInfoObj.setLibertyVersion(attribute.getValue().getValue());
                    case "JavaSpecVersion" ->
                            serverInfoObj.setJavaSpecificationVersion(attribute.getValue().getValue());
                    case "JavaRuntimeVersion" -> serverInfoObj.setJavaRuntimeVersion(attribute.getValue().getValue());
                }
            }
            return serverInfoObj;
        } catch (Exception e) {
            logger.error("Failed to get Server Info", e);
            return null;
        }
    }

    @Override
    public void stopServer() throws Exception {
        logger.debug("Stopping Server");
        jmxClient.shutdownFramework();
        logger.debug("Server shutdown initiated");
    }

    @Override
    public List<ApplicationStatus> getAllApplicationStatus() throws Exception {
        // Get all the applications MBeans
        // Loop through all the MBeans and get the attributes
        // Create a ApplicationStatus object and set the values
        // Add the ApplicationStatus object to the list
        List<ApplicationStatus> applicationStatusList = new ArrayList<>();

        jmxClient.getApplicationMBeans().forEach(application -> {
            ApplicationStatus applicationStatus = new ApplicationStatus();
            // The objectName with like "WebSphere:service=com.ibm.websphere.application.ApplicationMBean,name=sample" carve out the application name
            String applicationName = application.getObjectName().split("name=")[1];
            applicationStatus.setApplicationName(applicationName);
            try {
                List<Attribute> serverInfo = jmxClient.getMBeanAttributes(application);
                //Loop through all the attributes and set the values
                for (Attribute attribute : serverInfo) {
                    if (attribute.getName().equals("State")) {
                        applicationStatus.setStatus(attribute.getValue().getValue());
                        break;
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to get Application Status", e);
                throw new RuntimeException(e);
            }
            applicationStatusList.add(applicationStatus);
        });

        return applicationStatusList;
    }

    @Override
    public ApplicationStatus getApplicationStatus(String appName) {
        return null;
    }

    @Override
    public void restartAllApplications() throws Exception {
        jmxClient.getApplicationMBeans().forEach(application -> {
            try {
                jmxClient.invokeOperation(application, "restart");
            } catch (Exception e) {
                logger.error("Failed to restart application", e);
            }
        });
    }

    @Override
    public void restartApplication(String appName) {

    }
}
