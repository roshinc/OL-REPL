package dev.roshin.openliberty.repl.controllers.jmx.rest;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Verify;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import dev.roshin.openliberty.repl.controllers.jmx.rest.domain.MBeanInfo;
import dev.roshin.openliberty.repl.controllers.jmx.rest.domain.attributes.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

public class JmxClient {
    private final HttpClient client;
    private final URL baseUrl;
    private final String authHeader;
    private final int retries;
    private final Duration timeout;
    private final Logger logger;
    private static final String EMPTY_POST_BODY = "{\"params\":[],\"signature\":[]}";
    private static final Duration DURATION_BETWEEN_REQUESTS = Duration.ofSeconds(1);
    private static final String MBEAN_URL_CONTEXT_ROOT = "/IBMJMXConnectorREST/mbeans";


    private final Gson gson = new Gson();

    public JmxClient(String host, int port, String username, String password, Duration timeout, int retries) throws Exception {
        this(new URL("https://" + host + ":" + port), username, password, timeout, retries);
    }

    public JmxClient(URL baseUrl, String username, String password, Duration timeout, int retries) throws NoSuchAlgorithmException, KeyManagementException, MalformedURLException {
        this.logger = LoggerFactory.getLogger(getClass());
        logger.debug("Starting JmxClient constructor");

        Preconditions.checkNotNull(baseUrl, "Base URL cannot be null");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(username), "Username cannot be null or empty");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(password), "Password cannot be null or empty");
        Preconditions.checkNotNull(timeout, "Timeout cannot be null");
        Preconditions.checkArgument(retries >= 0, "Retries cannot be negative");

        // Trust all certificates
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
        // Create HTTP client, with the SSL context and timeout
        this.client = HttpClient.newBuilder()
                .sslContext(sslContext)
                .connectTimeout(timeout)
                .build();
        // Create base URL, without the context root
        this.baseUrl = new URL(baseUrl.getProtocol(), baseUrl.getHost(), baseUrl.getPort(), "");

        // Create Basic Authentication header
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
        this.authHeader = "Basic " + new String(encodedAuth);

        this.timeout = timeout;
        this.retries = retries;
    }


    /**
     * Sends a GET or POST request to the specified URL
     * <p>
     * If the request fails, it will retry the request {@link JmxClient#retries} times
     * If the request fails after {@link JmxClient#retries} times, it will throw an exception
     * If the request succeeds, it will return the response body
     * If the response status code is 400 or above, it will throw an exception
     *
     * @param url                     The URL to send the request to
     * @param shouldPostWithEmptyBody Whether the request should be a POST request with an empty body
     * @return The response body, if the request succeeds
     * @throws InterruptedException If the request is interrupted
     * @throws IOException          If the request fails
     * @throws URISyntaxException   If the URL is invalid
     */
    protected String sendRequest(String url, boolean shouldPostWithEmptyBody) throws InterruptedException, IOException, URISyntaxException {
        logger.debug("Starting sendRequest");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(url), "URL cannot be null or empty");

        logger.debug("Sending request to " + url);
        int localRetries = retries;
        for (int i = 0; i < localRetries; i++) {
            try {
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(new URI(url))
                        .header("Content-Type", "application/json")
                        .header("Authorization", authHeader)
                        .timeout(timeout);
                if (shouldPostWithEmptyBody) {
                    logger.debug("The request is a POST request");
                    logger.debug("Request body: {}", EMPTY_POST_BODY);
                    requestBuilder.POST(HttpRequest.BodyPublishers.ofString(EMPTY_POST_BODY));
                } else {
                    logger.debug("The request is a GET request");
                    requestBuilder.GET();
                }
                HttpRequest request = requestBuilder.build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                logger.debug("Response status code: {}", response.statusCode());
                logger.debug("Response body: {}", response.body());
                if (response.statusCode() >= 400) {
                    // Parse error response
                    JsonObject errorResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                    String error = errorResponse.get("error").getAsString();
                    String throwable = new String(Base64.getDecoder().decode(errorResponse.get("throwable").getAsString()));
                    logger.error("Request failed with error: " + error + ". Throwable: " + throwable);
                    throw new RuntimeException("Request failed with error: " + error);
                }
                return response.body();
            } catch (RuntimeException | IOException | URISyntaxException e) {
                logger.error("Request failed, might retry", e);
                if (i == localRetries - 1) {
                    throw e;
                }
                logger.debug("Retrying request in {} second(s)", DURATION_BETWEEN_REQUESTS.toSeconds());
                Thread.sleep(DURATION_BETWEEN_REQUESTS.toMillis());
            }
        }
        logger.error("Request failed after " + retries + " retries");
        throw new RuntimeException("Request failed after " + retries + " retries");
    }

    /**
     * Create a request URL that includes the base URL, optionally the mbean context root and the URL parts
     * <p>
     * The mbean context root does not end with a slash
     *
     * @param includeMBeanContextRoot Whether to include the mbean context root
     * @param urlParts                The URL parts
     * @return The request URL
     */
    protected String getRequestUrl(boolean includeMBeanContextRoot, String... urlParts) {
        logger.debug("Starting getRequestUrl");

        // Create the request URL
        StringBuilder url = new StringBuilder();
        url.append(baseUrl);
        if (includeMBeanContextRoot) {
            url.append(MBEAN_URL_CONTEXT_ROOT);
        }
        for (String urlPart : urlParts) {
            url.append(urlPart);
        }
        return url.toString();
    }


    /**
     * Get the list of MBeans
     *
     * @return List of {@link MBeanInfo} objects
     * @throws IOException          If the request fails
     * @throws URISyntaxException   If the URL is invalid
     * @throws InterruptedException If the request is interrupted
     */
    public List<MBeanInfo> getMBeans() throws IOException, URISyntaxException, InterruptedException {
        logger.debug("Starting getMBeans");
        String response = sendRequest(getRequestUrl(true, ""), false);
        logger.debug("Response from getMBeans: {}", response);
        return gson.fromJson(response, new TypeToken<List<MBeanInfo>>() {
        }.getType());
    }

    /**
     * Get the list of MBeans matching the given object name or class name
     *
     * @param objectNameQuery The object name query, can be null
     * @param classNameQuery  The class name query, can be null
     * @return List of {@link MBeanInfo} objects, can be empty
     * @throws IOException          If the request fails
     * @throws URISyntaxException   If the URL is invalid
     * @throws InterruptedException If the request is interrupted
     */
    public List<MBeanInfo> queryMBeans(String objectNameQuery, String classNameQuery) throws IOException, URISyntaxException, InterruptedException {
        logger.debug("Starting queryMBeans");

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
        String query = queryBuilder.toString();
        logger.debug("Query string: {}", query);

        String response = sendRequest(getRequestUrl(true, "?", query), false);
        logger.debug("Response from queryMBeans: {}", response);
        return gson.fromJson(response, new TypeToken<List<MBeanInfo>>() {
        }.getType());
    }

    /**
     * Get the MBean info for the given MBean
     *
     * @param mBeanInfo The {@link MBeanInfo} object, cannot be null
     * @return The MBean info as a {@link JsonObject}
     * @throws IOException          If the request fails
     * @throws URISyntaxException   If the URL is invalid
     * @throws InterruptedException If the request is interrupted
     */
    protected JsonObject getMBeanInfo(MBeanInfo mBeanInfo) throws IOException, URISyntaxException, InterruptedException {
        logger.debug("Starting getMBeanInfo");
        Preconditions.checkNotNull(mBeanInfo, "mBeanInfo cannot be null");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(mBeanInfo.getURL()), "mBeanInfo URL cannot be null or empty");

        String response = sendRequest(getRequestUrl(false, mBeanInfo.getURL()), false);
        logger.debug("Response from getMBeanInfo: {}", response);
        return JsonParser.parseString(response).getAsJsonObject();
    }

    /**
     * Get the MBean attributes for the given MBean
     *
     * @param mBeanInfo The {@link MBeanInfo} object, cannot be null
     * @return The list of {@link Attribute} objects
     * @throws IOException          If the request fails
     * @throws URISyntaxException   If the URL is invalid
     * @throws InterruptedException If the request is interrupted
     */
    public List<Attribute> getMBeanAttributes(MBeanInfo mBeanInfo) throws IOException, URISyntaxException, InterruptedException {
        logger.debug("Starting getMBeanAttributes");
        Preconditions.checkNotNull(mBeanInfo, "mBeanInfo cannot be null");
        // Get MBean info
        JsonObject serverInfoMBeanInfo = getMBeanInfo(mBeanInfo);
        // Get attribute url
        String attributeUrl = serverInfoMBeanInfo.get("attributes_URL").getAsString();
        logger.debug("Attribute URL: {}", attributeUrl);
        Verify.verify(!Strings.isNullOrEmpty(attributeUrl), "Attribute URL cannot be null or empty");

        // Get attributes
        String response = sendRequest(getRequestUrl(false, attributeUrl), false);
        logger.debug("Response from getMBeanAttributes: {}", response);
        return gson.fromJson(response, new TypeToken<List<Attribute>>() {
        }.getType());
    }


    /**
     * Invoke an operation on an MBean
     *
     * @param mBean     MBean to invoke the operation on, cannot be null
     * @param operation Operation to invoke, cannot be null or empty
     * @return true if the operation was invoked, false if the operation was not found or an error occurred
     * @throws Exception if thrown by {@link #getMBeanInfo(MBeanInfo)}
     */
    public boolean invokeOperation(MBeanInfo mBean, String operation) throws Exception {
        logger.debug("Invoking operation " + operation + " on MBean " + mBean);

        Preconditions.checkNotNull(mBean, "MBean cannot be null");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(operation), "Operation cannot be null or empty");

        // Get the MBean info
        JsonObject frameworkMBeanInfo = getMBeanInfo(mBean);
        // Get operations
        JsonArray operations = frameworkMBeanInfo.get("operations").getAsJsonArray();
        logger.debug("Operations: " + operations);

        // If there are no operations, return false
        if (operations.size() == 0) {
            logger.debug("No operations found");
            return false;
        }

        // Find the operation
        for (JsonElement operationElement : operations) {
            JsonObject operationObject = operationElement.getAsJsonObject();
            if (operationObject.get("name").getAsString().equals(operation)) {
                String operationURL = operationObject.get("URL").getAsString();
                logger.debug("Operation URL: " + operationURL);
                try {
                    String response = sendRequest(getRequestUrl(false, operationURL), true);
                    logger.debug("Response from " + operation + ": " + response);
                    return true;
                } catch (Exception e) {
                    logger.error("Error invoking operation " + operation + " on MBean " + mBean, e);
                    return false;
                }
            }
        }
        logger.debug("Operation " + operation + " not found");
        return false;
    }
}
