package dev.roshin.openliberty.repl.jmx.rest;

import com.google.common.base.Strings;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import dev.roshin.openliberty.repl.jmx.rest.domain.MBeanInfo;
import dev.roshin.openliberty.repl.jmx.rest.domain.attributes.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

public class JmxClient {
    private final HttpClient client;
    private final URL baseUrl;
    private final String authHeader;
    private final int retries;
    private final int timeout;
    private static final Logger logger = LoggerFactory.getLogger(JmxClient.class);

    public static final String SERVER_INFO_MBEAN_OBJECT_QUERY = "WebSphere:feature=kernel,name=ServerInfo";
    public static final String APPLICATION_MBEAN_OBJECT_QUERY = "WebSphere:service=com.ibm.websphere.application.ApplicationMBean,name=*";
    public static final String FRAMEWORK_MBEAN_OBJECT_QUERY = "osgi.core:type=framework,version=*,framework=org.eclipse.osgi,uuid=*";


    private final Gson gson = new Gson();

    public JmxClient(String host, int port, String username, String password, int timeout, int retries) throws Exception {
        this(new URL("https://" + host + ":" + port), username, password, timeout, retries);
    }

    public JmxClient(URL baseUrl, String username, String password, int timeout, int retries) throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }

                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
        };
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

        this.client = HttpClient.newBuilder()
                .sslContext(sslContext)
                .connectTimeout(Duration.ofSeconds(timeout))
                .build();
        this.baseUrl = new URL(baseUrl.getProtocol(), baseUrl.getHost(), baseUrl.getPort(), "");

        // Create Basic Authentication header
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
        this.authHeader = "Basic " + new String(encodedAuth);

        this.timeout = timeout;
        this.retries = retries;
    }

    private String sendRequest(String url, Optional<String> postBody) throws Exception {
        logger.debug("Sending request to " + url);
        for (int i = 0; i < retries; i++) {
            try {
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(new URI(url))
                        .header("Content-Type", "application/json")
                        .header("Authorization", authHeader)
                        .timeout(Duration.ofSeconds(timeout));
                if (postBody.isPresent()) {
                    logger.debug("The request is a POST request");
                    logger.debug("Request body: " + postBody.get());
                    requestBuilder.POST(HttpRequest.BodyPublishers.ofString(postBody.get()));
                } else {
                    logger.debug("The request is a GET request");
                    requestBuilder.GET();
                }
                HttpRequest request = requestBuilder.build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                logger.debug("Response status code: " + response.statusCode());
                logger.debug("Response body: " + response.body());
                if (response.statusCode() >= 400) {
                    // Parse error response
                    JsonObject errorResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                    String error = errorResponse.get("error").getAsString();
                    String throwable = new String(Base64.getDecoder().decode(errorResponse.get("throwable").getAsString()));
                    logger.error("Request failed with error: " + error + ". Throwable: " + throwable);
                    throw new Exception("Request failed with error: " + error);
                }
                return response.body();
            } catch (Exception e) {
                logger.error("Request failed", e);
                if (i == retries - 1) {
                    throw e;
                }
                // Optional: add a delay before retrying
                Thread.sleep(1000);
            }
        }
        throw new Exception("Request failed after " + retries + " retries");
    }


    public List<MBeanInfo> getListOfAllMBeans() throws Exception {
        String response = sendRequest(baseUrl + "/IBMJMXConnectorREST/mbeans/", Optional.empty());
        logger.info("Response from listAllMBeans: " + response);
        return gson.fromJson(response, new TypeToken<List<MBeanInfo>>() {
        }.getType());
    }

    public List<MBeanInfo> queryMBeans(String objectNameQuery, String classNameQuery) throws Exception {
        // Build the query string
        final StringBuilder queryBuilder = new StringBuilder();
        if (!Strings.isNullOrEmpty(objectNameQuery)) {
            queryBuilder.append("objectName=").append(objectNameQuery);
        }
        if (!Strings.isNullOrEmpty(classNameQuery)) {
            if (queryBuilder.length() > 0) {
                queryBuilder.append("&");
            }
            queryBuilder.append("className=").append(classNameQuery);
        }
        String response = sendRequest(baseUrl + "/IBMJMXConnectorREST/mbeans?" + queryBuilder.toString(), Optional.empty());
        logger.info("Response from queryMBeans: " + response);
        return gson.fromJson(response, new TypeToken<List<MBeanInfo>>() {
        }.getType());
    }

    public JsonObject getMBeanInfo(MBeanInfo mBeanInfo) throws Exception {
        String response = sendRequest(baseUrl + mBeanInfo.getURL(), Optional.empty());
        logger.info("Response from getMBeanInfo: " + response);
        return JsonParser.parseString(response).getAsJsonObject();
    }

    public List<Attribute> getMBeanAttributes(MBeanInfo mBeanInfo) throws Exception {
        JsonObject serverInfoMBeanInfo = getMBeanInfo(mBeanInfo);
        // Get attribute url
        String attributeUrl = serverInfoMBeanInfo.get("attributes_URL").getAsString();

        String response = sendRequest(baseUrl + attributeUrl, Optional.empty());
        logger.info("Response from getMBeanAttributes: " + response);
        return gson.fromJson(response, new TypeToken<List<Attribute>>() {
        }.getType());
    }


    public List<Attribute> getServerInfo() throws Exception {
        // Query for the ServerInfo MBean
        List<MBeanInfo> serverInfoMBeans = queryMBeans(SERVER_INFO_MBEAN_OBJECT_QUERY, null);
        if (serverInfoMBeans.size() != 1) {
            logger.error("Expected 1 ServerInfo MBean, found " + serverInfoMBeans.size());
            throw new Exception("Expected 1 ServerInfo MBean, found " + serverInfoMBeans.size());
        }
        MBeanInfo serverInfoMBean = serverInfoMBeans.get(0);

        return getMBeanAttributes(serverInfoMBean);
    }

    public List<MBeanInfo> getApplicationMBeans() throws Exception {
        // Query for the Application MBeans
        List<MBeanInfo> applicationMBeans = queryMBeans(APPLICATION_MBEAN_OBJECT_QUERY, null);
        logger.info("Application MBeans: " + applicationMBeans);
        return applicationMBeans;
    }

    public String getApplicationMbeanRestartOperationURL(MBeanInfo applicationMBean) throws Exception {
        JsonObject applicationMBeanInfo = getMBeanInfo(applicationMBean);
        // Get operations
        JsonArray operations = applicationMBeanInfo.get("operations").getAsJsonArray();
        // Find the restart operation
        for (JsonElement operation : operations) {
            JsonObject operationObject = operation.getAsJsonObject();
            if (operationObject.get("name").getAsString().equals("restart")) {
                return operationObject.get("URL").getAsString();
            }
        }
        throw new Exception("Could not find restart operation for application MBean " + applicationMBean);
    }

    public void restartApplication(MBeanInfo applicationMBean) throws Exception {
        String restartOperationURL = getApplicationMbeanRestartOperationURL(applicationMBean);
        String response = sendRequest(baseUrl + restartOperationURL, Optional.of("{\"params\":[],\"signature\":[]}"));
        logger.info("Response from restartApplication: " + response);
    }

    public MBeanInfo getFrameworkMBean() throws Exception {
        // Query for the Framework MBean
        List<MBeanInfo> frameworkMBeans = queryMBeans(FRAMEWORK_MBEAN_OBJECT_QUERY, null);
        if (frameworkMBeans.size() != 1) {
            logger.error("Expected 1 Framework MBean, found " + frameworkMBeans.size());
            throw new Exception("Expected 1 Framework MBean, found " + frameworkMBeans.size());
        }
        MBeanInfo frameworkMBean = frameworkMBeans.get(0);
        logger.info("Framework MBean: " + frameworkMBean);
        return frameworkMBean;
    }

    public String getFrameworkMBeanShutdownOperationURL() throws Exception {
        MBeanInfo frameworkMBean = getFrameworkMBean();
        JsonObject frameworkMBeanInfo = getMBeanInfo(frameworkMBean);
        // Get operations
        JsonArray operations = frameworkMBeanInfo.get("operations").getAsJsonArray();
        // Find the shutdown operation
        for (JsonElement operation : operations) {
            JsonObject operationObject = operation.getAsJsonObject();
            if (operationObject.get("name").getAsString().equals("shutdownFramework")) {
                return operationObject.get("URL").getAsString();
            }
        }
        throw new Exception("Could not find shutdownFramework operation");
    }

    public void shutdownFramework() throws Exception {
        String shutdownOperationURL = getFrameworkMBeanShutdownOperationURL();
        String response = sendRequest(baseUrl + shutdownOperationURL, Optional.of("{\"params\":[],\"signature\":[]}"));
        logger.info("Response from shutdownFramework: " + response);
    }

    public void invokeOperation(MBeanInfo mBean, String operation) throws Exception {
        JsonObject frameworkMBeanInfo = getMBeanInfo(mBean);
        // Get operations
        JsonArray operations = frameworkMBeanInfo.get("operations").getAsJsonArray();

        // Find the operation
        for (JsonElement operationElement : operations) {
            JsonObject operationObject = operationElement.getAsJsonObject();
            if (operationObject.get("name").getAsString().equals(operation)) {
                String operationURL = operationObject.get("URL").getAsString();
                String response = sendRequest(baseUrl + operationURL, Optional.of("{\"params\":[],\"signature\":[]}"));
                logger.info("Response from " + operation + ": " + response);
                return;
            }
        }
    }
}
