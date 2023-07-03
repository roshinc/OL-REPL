package dev.roshin.openliberty.repl.jmx.rest;

import com.google.common.net.UrlEscapers;
import dev.roshin.openliberty.repl.jmx.rest.domain.MBeanInfo;
import dev.roshin.openliberty.repl.jmx.rest.domain.attributes.Attribute;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JmxClientTest {

    private final String host = "localhost";
    private final int port = 9443;
    private final String username = "Todd";
    private final String password = "toddpassword";


    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void listAllMBeans() throws Exception {
        JmxClient client = new JmxClient(host, port, username, password, 10, 3);
        List<MBeanInfo> mbeans = client.getListOfAllMBeans();


        // Iterate through the list of MBeans
        for (MBeanInfo mbean : mbeans) {
            System.out.println("MBean: " + mbean);
        }
    }

    @Test
    void encodingTest() throws MalformedURLException {
        /*MBeanInfo{objectName='WebSphere:feature=kernel,name=ServerInfo',
            className='com.ibm.ws.kernel.server.internal.ServerInfoMBeanImpl',
            URL='/IBMJMXConnectorREST/mbeans/WebSphere%3Afeature%3Dkernel%2Cname%3DServerInfo'}*/
        String response = UrlEscapers.urlFormParameterEscaper().escape("WebSphere:type=ServletStats,*");
        System.out.println("Encoded: " + response);

        URL url = new URL("https://localhost:9443/IBMJMXConnectorREST");
        System.out.println("URL: " + url);
        // Remove the context root
        String path = url.getPath().substring(1);
        // Make it an url again
        url = new URL(url.getProtocol(), url.getHost(), url.getPort(), "");
        System.out.println("URL: " + url);
    }

    @Test
    void queryMBeans() throws Exception {
        JmxClient client = new JmxClient(host, port, username, password, 10, 3);
        List<MBeanInfo> mbeans = client.queryMBeans(JmxClient.SERVER_INFO_MBEAN_OBJECT_QUERY, null);

        assertNotNull(mbeans);
        assertEquals(1, mbeans.size());
    }

    @Test
    void getMBeanInfo() throws Exception {

        JmxClient client = new JmxClient(host, port, username, password, 10, 3);

        List<MBeanInfo> mbeans = client.queryMBeans(JmxClient.SERVER_INFO_MBEAN_OBJECT_QUERY, "");

        for (MBeanInfo mbean : mbeans) {
            System.out.println("MBean: " + mbean);
//            try {
//                List<Attribute> mbeanInfo = client.getMBeanInfo(mbean);
//                System.out.println("MBean info: " + mbeanInfo);
//            } catch (Exception e) {
//                System.out.println("Error: " + e.getMessage());
//            }
        }

//        List<MBeanInfo> mbeans = client.listAllMBeans();
//
//        // Iterate through the list of MBeans
//        for (MBeanInfo mbean : mbeans) {
//            System.out.println("MBean: " + mbean.getClassName());
//            try {
//                MBeanDetails mbeanInfo = client.getMBeanInfo(mbean);
//                System.out.println("MBean info: " + mbeanInfo.getDescription());
//            } catch (Exception e) {
//                System.out.println("Error: " + e.getMessage());
//            }
//        }
        // String server = client.getServerInfo();
        // System.out.println(server);

        /*
         String encoded = "rO0ABXNy..."; // your base64 string here
        byte[] data = Base64.getDecoder().decode(encoded);
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
            Object o = ois.readObject();
            System.out.println(o);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
         */

    }

    @Test
    void getServerInfo() throws Exception {
        JmxClient client = new JmxClient(host, port, username, password, 10, 3);
        List<Attribute> serverInfo = client.getServerInfo();
        assertNotNull(serverInfo);
        assertFalse(serverInfo.isEmpty());

        System.out.println("Server info: " + serverInfo);
    }

    @Test
    void getApplicationMBeans() throws Exception {
        JmxClient client = new JmxClient(host, port, username, password, 10, 3);
        List<MBeanInfo> mbeans = client.getApplicationMBeans();
        assertNotNull(mbeans);
        assertFalse(mbeans.isEmpty());

        System.out.println("Application MBeans: " + mbeans);

        // For each application MBean, get the attributes and print them
        for (MBeanInfo mbean : mbeans) {
            System.out.println("MBean: " + mbean);
            List<Attribute> attributes = client.getMBeanAttributes(mbean);
            System.out.println("Attributes: " + attributes);
        }
    }

    @Test
    void getApplicationMbeanRestartOperationURL() throws Exception {
        JmxClient client = new JmxClient(host, port, username, password, 10, 3);
        List<MBeanInfo> mbeans = client.getApplicationMBeans();
        assertNotNull(mbeans);
        assertFalse(mbeans.isEmpty());
        for (MBeanInfo mbean : mbeans) {
            System.out.println("MBean: " + mbean);
            String restartURL = client.getApplicationMbeanRestartOperationURL(mbean);
            System.out.println("Restart URL: " + restartURL);
        }
    }

    @Test
    void restartApplication() throws Exception {
        JmxClient client = new JmxClient(host, port, username, password, 10, 3);
        List<MBeanInfo> mbeans = client.getApplicationMBeans();
        assertNotNull(mbeans);
        assertFalse(mbeans.isEmpty());
        for (MBeanInfo mbean : mbeans) {
            System.out.println("MBean: " + mbean);
            String restartURL = client.getApplicationMbeanRestartOperationURL(mbean);
            System.out.println("Restart URL: " + restartURL);
            client.restartApplication(mbean);
        }
    }

    @Test
    void getFrameworkMBeans() throws Exception {
        JmxClient client = new JmxClient(host, port, username, password, 10, 3);
        MBeanInfo fwMBean = client.getFrameworkMBean();
        assertNotNull(fwMBean);
        // get all attributes
        List<Attribute> attributes = client.getMBeanAttributes(fwMBean);
        assertNotNull(attributes);
        // Print all attributes
        for (Attribute attribute : attributes) {
            System.out.println(attribute);
        }
    }

    @Test
    void shutdownFramework() throws Exception {
        JmxClient client = new JmxClient(host, port, username, password, 10, 3);
        client.shutdownFramework();
    }

}