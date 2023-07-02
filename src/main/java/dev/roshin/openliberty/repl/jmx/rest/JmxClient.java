package dev.roshin.openliberty.repl.jmx.rest;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

public class JmxClient {
    private final HttpClient client;
    private final String baseUrl;
    private final String authHeader;
    private final int retries;
    private final int timeout;
    private static final Logger logger = LoggerFactory.getLogger(JmxClient.class);

    private final Gson gson = new Gson();

    public JmxClient(String host, int port, String username, String password, int timeout, int retries) throws Exception {
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
        this.baseUrl = "https://" + host + ":" + port + "/IBMJMXConnectorREST/mbeans/";

        // Create Basic Authentication header
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
        this.authHeader = "Basic " + new String(encodedAuth);

        this.timeout = timeout;
        this.retries = retries;
    }

    private String sendRequest(String url) throws Exception {
        for (int i = 0; i < retries; i++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(url))
                        .header("Content-Type", "application/json")
                        .header("Authorization", authHeader)
                        .timeout(Duration.ofSeconds(timeout))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
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


    public List<MBeanInfo> listAllMBeans() throws Exception {
        String response = sendRequest(baseUrl);
        logger.info("Response from listAllMBeans: " + response);
        return gson.fromJson(response, new TypeToken<List<MBeanInfo>>() {
        }.getType());
    }

    public JsonObject getMBeanInfo(String mbeanName) throws Exception {
        String response = sendRequest(baseUrl + URLEncoder.encode(mbeanName, "UTF-8"));
        logger.info("Response from getMBeanInfo: " + response);
        return JsonParser.parseString(response).getAsJsonObject();
    }

    // Similarly, you can implement methods to get/set attributes, invoke operations, etc.
}
