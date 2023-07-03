package dev.roshin.openliberty.repl.util;

import com.google.common.base.Preconditions;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class ServerSourceUtils {

    private static final Logger logger = LoggerFactory.getLogger(ServerSourceUtils.class);

    private ServerSourceUtils() {
        // Private constructor
    }

    /**
     * Gets the server source from the user, from the server-sources directory
     *
     * @param terminal The terminal to use
     * @return The path of the server source
     * @throws IOException If there is an error reading the server-sources directory
     */
    public static Path pickServerSource(Terminal terminal) throws IOException {
        logger.debug("Picking server source");

        // Create a writer
        PrintWriter writer = terminal.writer();

        // Path of the directory
        String currentWorkingDirectory = System.getProperty("user.dir");
        Path directory = Paths.get(currentWorkingDirectory, "server-sources");
        logger.debug("Looking for server sources in: {}", directory);
        logger.debug("Creating the directory if it doesn't exist");
        // Create the directory if it doesn't exist
        Files.createDirectories(directory);

        // Create a string builder
        AttributedStringBuilder asb = new AttributedStringBuilder();
        // Append the location of the server sources
        asb.append("Looking for server sources in: ");
        asb.append(directory.toString(), AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
        // Print the message
        writer.println(asb.toAnsi());

        // Get sub-folders
        List<String> subfolders;
        try {
            subfolders = Files.walk(directory, 1)
                    .filter(Files::isDirectory)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Unable to read server-sources", e);
            throw new IllegalStateException("Unable to read server-sources", e);
        }

        // Remove the first element because it's the parent directory itself
        subfolders = subfolders.subList(1, subfolders.size());

        // If there are no sub-folders, write a message return null
        if (subfolders.isEmpty()) {
            TerminalUtils.printErrorMessages("No server sources found", terminal);
            logger.error("There are no server sources, throwing exception");
            throw new RuntimeException("There are no server sources");
        }

        final String selectedFolder = TerminalUtils.promptUserForSelection(terminal, subfolders, "server source");

        logger.debug("User selected: {}", selectedFolder);
        Path selectedServerSource = directory.resolve(selectedFolder);

        // Validate the server source
        validateServerSource(selectedServerSource, terminal);

        return selectedServerSource;
    }

    /**
     * Validates the server source
     * <p>
     *     The server source must contain the following:
     *     <ul>
     *         <li>A valid pom.xml file</li>
     *         <li>A valid src/main/liberty/config/server.xml file</li>
     *         <li>A maven wrapper for the current OS</li>
     *     </ul>
     *     If any of the above are not present, the server source is not valid
     *
     * @param serverSource The path of the server source
     * @param terminal     The terminal to use
     */
    private static void validateServerSource(Path serverSource, Terminal terminal) {
        logger.debug("Validating server source: {}", serverSource);

        Preconditions.checkNotNull(serverSource, "Server source cannot be null");
        Preconditions.checkNotNull(terminal, "Terminal cannot be null");

        // Create writer
        PrintWriter writer = terminal.writer();

        // Inform the user that the server source is being validated
        writer.println("Validating the server source...");

        // Check if this is a maven project
        if (!Files.exists(serverSource.resolve("pom.xml"))) {
            logger.debug("The server source at {}, does not contain a pom.xml file", serverSource);
            TerminalUtils.printErrorMessages("The selected server source is not valid", terminal);
            throw new IllegalStateException("The selected server source does not contain a pom.xml file");
        }

        // Validate the pom.xml file
        logger.debug("Validating the pom.xml file");
        if(!validateServerSourcePom(serverSource)){
            logger.debug("The server source at {}, does not contain a valid pom.xml file", serverSource);
            TerminalUtils.printErrorMessages("The selected server source is not valid", terminal);
            throw new IllegalStateException("The selected server source does not contain a valid pom.xml file");
        }

        // Check if the server source contains the src/main/liberty/config/server.xml file
        if (!Files.exists(serverSource.resolve("src/main/liberty/config/server.xml"))) {
            logger.debug("The server source at {}, does not contain a server.xml file", serverSource);
            TerminalUtils.printErrorMessages("The selected server source is not valid", terminal);
            throw new IllegalStateException("The selected server source does not contain the server.xml file");
        }

        // Check if the server contains the maven wrapper for the current OS
        if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
            if (!Files.exists(serverSource.resolve("mvnw.cmd"))) {
                logger.debug("The server source at {}, does not contain a maven wrapper for Windows", serverSource);
                TerminalUtils.printErrorMessages("The selected server source is not valid", terminal);
                throw new IllegalStateException("The selected server source does not contain the maven wrapper for Windows");
            }
        } else { // Linux or Mac
            if (!Files.exists(serverSource.resolve("mvnw"))) {
                logger.debug("The server source at {}, does not contain a maven wrapper for Linux or Mac", serverSource);
                TerminalUtils.printErrorMessages("The selected server source is not valid", terminal);
                throw new IllegalStateException("The selected server source does not contain the maven wrapper for Linux or Mac");
            }
        }

        logger.debug("The server source is valid");
        logger.debug("Checking if the server source is a git repository");

        // Check if the server source is a git repository
        // Check if the selected directory is a Git repository and fetch the latest commit hash and message
        ProcessBuilder gitRevParseProcessBuilder = new ProcessBuilder();
        gitRevParseProcessBuilder.command("git", "rev-parse", "--is-inside-work-tree");
        gitRevParseProcessBuilder.directory(serverSource.toFile());

        try {
            Process gitRevParseProcess = gitRevParseProcessBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(gitRevParseProcess.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if ("true".equals(line)) {
                    // It's a git repository, print the latest commit hash and message
                    ProcessBuilder gitLogProcessBuilder = new ProcessBuilder();
                    gitLogProcessBuilder.command("git", "log", "-1", "--pretty=format:%h - %s");
                    Process gitLogParseProcess = gitLogProcessBuilder.start();
                    reader = new BufferedReader(new InputStreamReader(gitLogParseProcess.getInputStream()));
                    while ((line = reader.readLine()) != null) {
                        writer.println(
                                new AttributedStringBuilder()
                                        .append("Latest commit: ")
                                        .append(line, AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                                        .append("\n")
                                        .toAnsi()
                        );
                    }
                } else {
                    // It's not a git repository
                    logger.debug("The server source at {}, is not a git repository", serverSource);
                    TerminalUtils.printErrorMessages("The selected server source is not valid", terminal);
                    throw new IllegalStateException("The selected server source is not a git repository");
                }
            }
        } catch (IOException e) {
            logger.error("Error while checking Git repository", e);
            TerminalUtils.printErrorMessages("The selected server source is not valid", terminal);
            throw new RuntimeException("Error while checking Git repository", e);
        }
    }

    /**
     * Validates the pom.xml file of the server source
     * <p>
     *     The pom.xml file must contain the liberty-maven-plugin
     *
     * @param serverSource The server source
     * @return True if the pom.xml file is valid, false otherwise
     */
    private static boolean validateServerSourcePom(Path serverSource) {
        logger.debug("Validating the pom.xml file at {}", serverSource);
        // Check if the pom.xml file contains the liberty-maven-plugin
        Path pomFile = serverSource.resolve("pom.xml");
        try {
            List<String> lines = Files.readAllLines(pomFile);
            boolean found = false;
            for (String line : lines) {
                if (line.contains("liberty-maven-plugin")) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                logger.debug("The pom.xml file does not contain the liberty-maven-plugin");
                return false;
            }
        } catch (IOException e) {
            logger.error("Unable to read the pom.xml file", e);
            return false;
        }
        return true;
    }


}
