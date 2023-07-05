package dev.roshin.openliberty.repl.controllers.jmx;

import dev.roshin.openliberty.repl.controllers.jmx.domain.ApplicationStatus;
import dev.roshin.openliberty.repl.controllers.jmx.domain.ServerInfo;
import dev.roshin.openliberty.repl.controllers.jmx.rest.JMXConstants;
import dev.roshin.openliberty.repl.controllers.jmx.rest.JmxClient;
import dev.roshin.openliberty.repl.controllers.jmx.rest.domain.MBeanInfo;
import dev.roshin.openliberty.repl.controllers.jmx.rest.domain.attributes.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class JMXServerManagerImpl implements JMXServerManager {

    private final URL baseURL;
    private final String username;
    private final String password;
    private final Duration timeout;
    private final int retries;
    private JmxClient jmxClient;

    private final Logger logger;

    public JMXServerManagerImpl(URL baseURL, String username, String password) throws Exception {
        this(baseURL, username, password, Duration.ofSeconds(5000), 3);
    }

    public JMXServerManagerImpl(URL baseURL, String username, String password, Duration timeout, int retries) throws Exception {
        this.baseURL = baseURL;
        this.username = username;
        this.password = password;
        this.timeout = timeout;
        this.retries = retries;

        this.jmxClient = new JmxClient(baseURL, username, password, timeout, retries);
        this.logger = LoggerFactory.getLogger(getClass());
    }

    /**
     * Preforms the shutdown operation on the Framework MBean
     */
    private void shutdownFrameworkInternal() {
        logger.debug("Shutting down framework internal");
        try {
            // Get the Framework MBeans
            List<MBeanInfo> frameworkMBeans = jmxClient.queryMBeans(JMXConstants.FRAMEWORK_MBEAN_OBJECT_QUERY, null);
            // We should get only one MBean
            if (frameworkMBeans.size() != 1) {
                logger.error("Expected 1 Framework MBean, found " + frameworkMBeans.size());
                throw new RuntimeException("Expected 1 Framework MBean, found " + frameworkMBeans.size());
            }
            MBeanInfo frameworkMBean = frameworkMBeans.get(0);
            // Invoke the shutdown operation
            logger.debug("Invoking shutdown operation");
            jmxClient.invokeOperation(frameworkMBean, JMXConstants.FRAMEWORK_MBEAN_SHUTDOWN_OPERATION);
        } catch (Exception e) {
            logger.error("Failed to shutdown framework", e);
            throw new RuntimeException("Failed to shutdown framework", e);
        }
    }

    /**
     * Get the ServerInfo MBean attributes
     *
     * @return List of attributes
     * @throws IOException          If there is an error while connecting to the JMX Server
     * @throws URISyntaxException   If there is an error while parsing the URL
     * @throws InterruptedException If there is an error while waiting for the response
     */
    private List<Attribute> getServerInfoInternal() throws IOException, URISyntaxException, InterruptedException {
        logger.debug("Getting Server Info Internal");
        // Get the ServerInfo MBean
        List<MBeanInfo> serverInfoMBeans = jmxClient.queryMBeans(JMXConstants.SERVER_INFO_MBEAN_OBJECT_QUERY, null);
        // We should get only one MBean
        if (serverInfoMBeans.size() != 1) {
            logger.error("Expected 1 ServerInfo MBean, found " + serverInfoMBeans.size());
            throw new RuntimeException("Expected 1 ServerInfo MBean, found " + serverInfoMBeans.size());
        }
        MBeanInfo serverInfoMBean = serverInfoMBeans.get(0);
        // Get all the attributes of the ServerInfo MBean
        return jmxClient.getMBeanAttributes(serverInfoMBean);
    }

    /**
     * Get all the Application MBeans
     *
     * @return List of MBeanInfo
     */
    private List<MBeanInfo> getApplicationMBeansInternal() {
        logger.debug("Starting getApplicationMBeansInternal");
        try {
            return jmxClient.queryMBeans(JMXConstants.APPLICATION_MBEAN_OBJECT_QUERY, null);
        } catch (Exception e) {
            logger.error("Failed to get Application MBeans", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isConnectable() {
        logger.debug("Trying to connect to JMX Server");
        // Try to get all MBeans, if we get an ConnectException, return false
        try {
            jmxClient.getMBeans();
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
            List<Attribute> serverInfo = getServerInfoInternal();
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
    public boolean stopServer() {
        logger.debug("Starting stopServer");
        if (!isConnectable()) {
            logger.error("Failed to connect to JMX Server");
            return false;
        }
        logger.debug("Stopping Server");
        shutdownFrameworkInternal();
        logger.debug("Server shutdown initiated");
        return isConnectable();
    }


    @Override
    public List<ApplicationStatus> getAllApplicationStatus() throws Exception {
        // Get all the applications MBeans
        // Loop through all the MBeans and get the attributes
        // Create a ApplicationStatus object and set the values
        // Add the ApplicationStatus object to the list
        List<ApplicationStatus> applicationStatusList = new ArrayList<>();

        getApplicationMBeansInternal().forEach(application -> {
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
    public void restartAllApplications() throws Exception {
        logger.debug("Starting restartAllApplications");

        getApplicationMBeansInternal().forEach(application -> {
            try {
                jmxClient.invokeOperation(application, JMXConstants.APPLICATION_MBEAN_RESTART_OPERATION);
            } catch (Exception e) {
                logger.error("Failed to restart application", e);
            }
        });
    }
}
