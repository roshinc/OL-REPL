package dev.roshin.openliberty.repl.jmx.rest;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

class JmxClientTest {

    private final String host = "localhost";
    private final int port = 9443;
    private final String username = "todd";
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
        List<MBeanInfo> mbeans = client.listAllMBeans();


        // Iterate through the list of MBeans
        for (MBeanInfo mbean : mbeans) {
            System.out.println("MBean: " + mbean.getClassName());
        }

        //System.out.println("All MBeans: " + mbeans);

        // JsonObject mbeanInfo = client.getMBeanInfo("WebSphere:type=ServletStats,*");
        //System.out.println("MBean info: " + mbeanInfo);
    }

    @Test
    void getMBeanInfo() throws Exception {

        JmxClient client = new JmxClient(host, port, username, password, 10, 3);


        JsonObject mbeanInfo = client.getMBeanInfo("org.apache.aries.jmx.framework.ServiceState");
        System.out.println("MBean info: " + mbeanInfo);

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
}