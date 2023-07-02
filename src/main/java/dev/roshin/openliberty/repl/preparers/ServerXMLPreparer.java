package dev.roshin.openliberty.repl.preparers;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.jline.terminal.Terminal;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;

public class ServerXMLPreparer {

    private ServerXMLPreparer() {
    }

    /**
     * Prepare the server.xml file for the given server source directory.
     * <p>
     * This method will add the needed features, users, groups and ports to the server.xml file.
     *
     * @param serverSource The server source directory
     * @param terminal     The terminal to print messages to
     */
    public static void prepareServerXML(Path serverSource, Terminal terminal) {
        // Inform the user that the server xml is being prepared
        terminal.writer().println("Preparing server.xml...");

        // Load the server.xml file
        Path serverXml = serverSource.resolve("src/main/liberty/config/server.xml");
        // Create jdom2 document for the server.xml file
        Document serverXmlDocument;
        try {
            serverXmlDocument = new SAXBuilder().build(serverXml.toFile());
        } catch (JDOMException | IOException e) {
            throw new RuntimeException("Error while loading the server.xml file", e);
        }

        //== Features configuration ==

        // Get the features element
        Element featuresElement = serverXmlDocument.getRootElement().getChild("featureManager");
        // Add the needed features, if they are not already present
        addFeatureIfNotPresent(featuresElement, "restConnector-2.0", terminal);
        addFeatureIfNotPresent(featuresElement, "adminCenter-1.0", terminal);

        //== Basic registry configuration ==

        // Get the basicRegistry element
        Element basicRegistryElement = serverXmlDocument.getRootElement().getChild("basicRegistry");
        // Add our user and group to the basicRegistry element
        addUsersAndGroupsIfNotPresent(basicRegistryElement, "todd", "toddpassword", "managers", terminal);


        //== Administrator role configuration ==
        // Get the administrator-role element
        var administratorRoleElement = serverXmlDocument.getRootElement().getChild("administrator-role");
        addGroupToAdministratorRoleIfNotPresent(administratorRoleElement, "managers");


        //== Port configuration ==
        // Get the httpEndpoint element
        Element httpEndpointElement = serverXmlDocument.getRootElement().getChild("httpEndpoint");
        // Get the httpPort attribute
        String httpPort = httpEndpointElement.getAttributeValue("httpPort");
        // Get the httpsPort attribute
        String httpsPort = httpEndpointElement.getAttributeValue("httpsPort");

        // Check if these ports are already in use, if it is get the next available port
        httpPort = getAvailablePort(httpPort);
        httpsPort = getAvailablePort(httpsPort);
        // the httpsPort should not be the same as the httpPort
        if (httpPort.equals(httpsPort)) {
            httpsPort = getAvailablePort(httpsPort + 1);
        }

        // Update the server.xml file with the new ports
        httpEndpointElement.setAttribute("httpPort", httpPort);
        httpEndpointElement.setAttribute("httpsPort", httpsPort);

        // Inform the user about the ports
        terminal.writer().println("Using httpPort: " + httpPort);
        terminal.writer().println("Using httpsPort: " + httpsPort);

        // Save the server.xml file
        try {
            new XMLOutputter().output(serverXmlDocument, Files.newOutputStream(serverXml));
        } catch (IOException e) {
            throw new RuntimeException("Error while saving the server.xml file", e);
        }


    }

    /**
     * Add the given group to the administrator-role element if it is not already present
     *
     * @param administratorRoleElement The administrator-role element
     * @param groupName                The name of the group to add
     */
    private static void addGroupToAdministratorRoleIfNotPresent(Element administratorRoleElement, String groupName) {
        // Check if the administrator-role element has a group element with the name "groupName"
        if (administratorRoleElement.getChildren().stream().noneMatch(e -> e.getText().equals(groupName))) {
            // Add the group element with the name "managers" to the administrator-role element
            administratorRoleElement.addContent(new org.jdom2.Element("group").setText(groupName));
        }
    }

    /**
     * Add the given user and group to the basic registry if they are not already present
     *
     * @param basicRegistryElement The basic registry element
     * @param userName             The name of the user to add
     * @param password             The password of the user to add
     * @param groupName            The name of the group to add the user to and to add to the basic registry
     * @param terminal             The terminal to print messages to
     */
    private static void addUsersAndGroupsIfNotPresent(Element basicRegistryElement, final String userName, final String password, final String groupName, Terminal terminal) {
        // Check if there is a user with the name "userName" and the password "password"
        if (basicRegistryElement.getChildren().stream().noneMatch(e -> e.getAttributeValue("name").equals(userName) && e.getAttributeValue("password").equals(password))) {
            // Add the user with the name "userName" and the password "password"
            basicRegistryElement.addContent(new Element("user").setAttribute("name", userName).setAttribute("password", password));
            // Print a message to the user
            terminal.writer().println("Added user to basic registry: " + userName);
        } else {
            // Print a message to the user
            terminal.writer().println("User " + userName + " already present in basic registry");
        }

        //Check if there is a group with the name "groupName"
        if (basicRegistryElement.getChildren().stream().noneMatch(e -> e.getAttributeValue("name").equals(groupName))) {
            // Add the group with the name "groupName"
            basicRegistryElement.addContent(new Element("group").setAttribute("name", groupName));
            // Print a message to the user
            terminal.writer().println("Added group to basic registry" + groupName);
        } else {
            // Print a message to the user
            terminal.writer().println("Group " + groupName + " already present in basic registry");
        }

        // Check if the group with the name "groupName" has a member element with the name "userName"
        if (basicRegistryElement.getChildren().stream().noneMatch(e -> e.getAttributeValue("name").equals(groupName) && e.getChildren().stream().anyMatch(c -> c.getAttributeValue("name").equals(userName)))) {
            // Add the member element with the name "todd" to the group with the name "managers"
            basicRegistryElement.getChildren().stream().filter(e -> e.getAttributeValue("name").equals(groupName)).findFirst().get().addContent(new Element("member").setAttribute("name", userName));
        }
    }

    /**
     * Add a feature to the features element if it is not present
     *
     * @param featuresElement The features element
     * @param featureName     The name of the feature to add
     * @param terminal        The terminal to print messages to the user
     */
    private static void addFeatureIfNotPresent(Element featuresElement, final String featureName, Terminal terminal) {
        // Check if the features has a feature element with value featureName
        if (featuresElement.getChildren().stream().noneMatch(e -> e.getText().equals(featureName))) {
            // Add the featureName feature
            featuresElement.addContent(new org.jdom2.Element("feature").setText(featureName));
            // Print a message to the user
            terminal.writer().println("Added feature " + featureName);
        } else {
            // Print a message to the user
            terminal.writer().println("Feature " + featureName + " already present");
        }
    }


    private static String getAvailablePort(final String httpPortString) {

        // Check if the port is already in use
        int httpPort;
        try {
            httpPort = Integer.parseInt(httpPortString);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid port number: " + httpPortString, e);
        }

        for (int attempts = 0; attempts < 10; attempts++) {
            try {
                try (var ignored = new ServerSocket(httpPort)) {
                    // The port is free, return it
                    return String.valueOf(httpPort);
                }
            } catch (IOException e) {
                // The port is in use, try the next one
                httpPort++;
            }
        }

        // If we tried 10 times without finding an open port, throw an exception
        throw new RuntimeException("Could not find an open port after 10 attempts");
    }
}
