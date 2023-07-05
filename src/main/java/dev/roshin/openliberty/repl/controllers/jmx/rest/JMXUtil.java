package dev.roshin.openliberty.repl.controllers.jmx.rest;

import dev.roshin.openliberty.repl.util.TerminalUtils;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
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

    public static URL findRestConnectorURL(Path serverSource, Terminal terminal) throws MalformedURLException {
        logger.debug("Starting findRestConnectorURL");

        // Create path to target directory
        Path target = serverSource.resolve("target");
        // Create path to liberty-plugin-config.xml file
        Path libertyPluginConfig = target.resolve("liberty-plugin-config.xml");
        // Load the liberty-plugin-config.xml file
        // Create jdom2 document for the liberty-plugin-config.xml file
        Document libertyPluginConfigDocument;
        try {
            libertyPluginConfigDocument = new SAXBuilder().build(libertyPluginConfig.toFile());
        } catch (JDOMException | IOException e) {
            // Inform the user that the liberty-plugin-config.xml file could not be loaded
            TerminalUtils.printErrorMessages("Error while loading the liberty-plugin-config.xml file", terminal);
            logger.debug("Error while loading the liberty-plugin-config.xml file", e);
            throw new RuntimeException("Error while loading the liberty-plugin-config.xml file", e);
        }
        // Get the server output directory from the liberty-plugin-config.xml file
        String serverOutputDirectory = libertyPluginConfigDocument.getRootElement().getChild("serverOutputDirectory").getText();
        // Create path to ${server.output.dir}/logs/state/com.ibm.ws.jmx.rest.address file
        Path jmxRestAddress = target.resolve(serverOutputDirectory).resolve("logs/state/com.ibm.ws.jmx.rest.address");
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
