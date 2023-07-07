package dev.roshin.openliberty.repl.controllers.jmx.rest;

import dev.roshin.openliberty.repl.config.generated.LibertyPluginConfigs;
import dev.roshin.openliberty.repl.util.TerminalUtils;
import org.jline.terminal.Terminal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class JMXUtil {

    private static final Logger logger = LoggerFactory.getLogger(JMXUtil.class);

    public static URL findRestConnectorURL(LibertyPluginConfigs libertyPluginConfigs, Terminal terminal) throws MalformedURLException {
        logger.debug("Starting findRestConnectorURL");

        // Get the server output directory from the liberty-plugin-config.xml file
        Path serverOutputDirectory = libertyPluginConfigs.getServerOutputDirectory();
        // Create path to ${server.output.dir}/logs/state/com.ibm.ws.jmx.rest.address file
        Path jmxRestAddress = (serverOutputDirectory).resolve("logs/state/com.ibm.ws.jmx.rest.address");
        // Load the com.ibm.ws.jmx.rest.address file
        String restConnectorURL;
        try {
            restConnectorURL = Files.readString(jmxRestAddress);
        } catch (IOException e) {
            // Inform the user that the com.ibm.ws.jmx.rest.address file could not be loaded
            TerminalUtils.printErrorMessages("Error while loading the com.ibm.ws.jmx.rest.address file", terminal);
            logger.debug("Error while loading the com.ibm.ws.jmx.rest.address file", e);
            throw new RuntimeException("Error while loading the com.ibm.ws.jmx.rest.address file", e);
        }
        // The url format will be service:jmx:rest://localhost:9443/IBMJMXConnectorREST, so we need to replace the service:jmx:rest:// with https
        restConnectorURL = restConnectorURL.replace("service:jmx:rest://", "https://");
        // Create a URL object from the restConnectorURL string
        return new URL(restConnectorURL);
    }
}
