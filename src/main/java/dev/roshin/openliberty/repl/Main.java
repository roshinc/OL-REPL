/**
 *
 */
package dev.roshin.openliberty.repl;

import dev.roshin.openliberty.repl.jmx.rest.JMXUtil;
import dev.roshin.openliberty.repl.preparers.ServerXMLPreparer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 *
 */
public class Main {

    // The maven process
    private static Process mavenProcess = null;
    private static volatile boolean serverReady = false;

    public static void main(String[] args) throws IOException {

        // Create a terminal
        Terminal terminal = TerminalBuilder.builder().system(true).build();

        // Print the introduction
        printIntroduction(terminal);

        // Pick the server source
        Path serverSource = pickServerSource(terminal);


        // Validate the server source
        validateServerSource(serverSource, terminal);

        // Prepare the server source
        prepareServerSource(serverSource, terminal);

        // exit for testing
//        System.exit(0);

        // Create a file name for the log file unique to this run
        String logFileName = "maven_log_" + System.currentTimeMillis() + ".txt";
        // Store the log file in a temporary directory within the current working directory
        Path mavenLogFile = Paths.get(System.getProperty("user.dir")).resolve("temp").resolve(logFileName);
        // Create the parent directories for the log file, if they do not exist
        Files.createDirectories(mavenLogFile.getParent());


        try {
            // Start the Maven process
            mavenProcess = startMavenProcess(serverSource, mavenLogFile, terminal);

            // Inform the user we are waiting for 30 seconds, with the word "waiting" in yellow
            AttributedStringBuilder attributedStringBuilder = new AttributedStringBuilder();
            attributedStringBuilder.append("Waiting standard ");
            attributedStringBuilder.append(String.valueOf(30), AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW));
            attributedStringBuilder.append(" seconds for server to become ready", AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW));
            terminal.writer().println(attributedStringBuilder.toAnsi());


            // Wait for 30 seconds
            TimeUnit.SECONDS.sleep(30);

            // Inform the user we are waiting for 30 seconds, with the word "waiting" in yellow
            AttributedStringBuilder configuredTimeoutStringBuilder = new AttributedStringBuilder();
            configuredTimeoutStringBuilder.append("Polling for configured timeout of ");
            configuredTimeoutStringBuilder.append(String.valueOf(60), AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW));
            configuredTimeoutStringBuilder.append(" seconds for server to become ready", AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW));
            terminal.writer().println(configuredTimeoutStringBuilder.toAnsi());

            // Start a separate thread to monitor the log file
            Thread logMonitorThread = new Thread(() -> monitorLogFile(mavenLogFile.toFile()));
            logMonitorThread.start();

            // Wait for server to become ready or timeout after 60 seconds
            for (int i = 0; i < 60; i++) {
                if (serverReady) {
                    break;
                }
                TimeUnit.SECONDS.sleep(1);
            }
            if (!serverReady) {
                throw new RuntimeException("Server did not become ready within 60 seconds");
            }

            // Inform the user that the server is ready, with the word "ready" in green
            AttributedStringBuilder serverReadyStringBuilder = new AttributedStringBuilder();
            serverReadyStringBuilder.append("Server ready", AttributedStyle.BOLD.foreground(AttributedStyle.GREEN));
            terminal.writer().println(serverReadyStringBuilder.toAnsi());

            // Write the jmx url to the terminal
            terminal.writer().println("JMX URL: " + JMXUtil.findRestConnectorURL(serverSource, terminal));


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // If the server was not stopped in a normal way, or if an error occurred,
            // the Maven process is killed here
            if (mavenProcess != null && mavenProcess.isAlive()) {
                mavenProcess.destroyForcibly();
            }
        }

    }

    /**
     * Prints the introduction to the terminal, including the ascii art and the version number
     *
     * @param terminal The terminal to print the introduction to
     */
    private static void printIntroduction(Terminal terminal) {
        String asciiArt = " ______     __            ______     ______     ______   __        \n" +
                "/\\  __ \\   /\\ \\          /\\  == \\   /\\  ___\\   /\\  == \\ /\\ \\       \n" +
                "\\ \\ \\/\\ \\  \\ \\ \\____     \\ \\  __<   \\ \\  __\\   \\ \\  _-/ \\ \\ \\____  \n" +
                " \\ \\_____\\  \\ \\_____\\     \\ \\_\\ \\_\\  \\ \\_____\\  \\ \\_\\    \\ \\_____\\ \n" +
                "  \\/_____/   \\/_____/      \\/_/ /_/   \\/_____/   \\/_/     \\/_____/";

        // Create a writer for the terminal
        PrintWriter tWriter = terminal.writer();


        // Create a new AttributedStringBuilder
        AttributedStringBuilder attributedStringBuilder = new AttributedStringBuilder();
        // Print a new line
        tWriter.println();
        // Append the ascii art to the AttributedStringBuilder
        attributedStringBuilder.append(asciiArt, AttributedStyle.BOLD.foreground(AttributedStyle.GREEN));
        // Print the AttributedStringBuilder to the terminal
        terminal.writer().println(attributedStringBuilder.toAnsi());

        // Create a new AttributedStringBuilder
        attributedStringBuilder = new AttributedStringBuilder();
        // Append the introduction text with version number to the AttributedStringBuilder
        attributedStringBuilder.append("Open Liberty REPL ");
        attributedStringBuilder.append("v1.0.0", AttributedStyle.BOLD.foreground(AttributedStyle.GREEN));
        // Print the AttributedStringBuilder to the terminal
        terminal.writer().println(attributedStringBuilder.toAnsi());

        // Print a new line
        tWriter.println();
        // Print the starting up text to the AttributedStringBuilder
        tWriter.println("Starting up...");
        // Print a new line
        tWriter.println();

        terminal.flush();
    }

    private static void prepareServerSource(Path serverSource, Terminal terminal) {

        // Inform the user that the server source is being prepared
        terminal.writer().println("Preparing server source...");

        // Prep server.xml
        ServerXMLPreparer.prepareServerXML(serverSource, terminal);
    }


    private static void validateServerSource(Path serverSource, Terminal terminal) {
        // Inform the user that the server source is being validated
        terminal.writer().println("Validating the server source...");

        // Check if this is a maven project
        if (!Files.exists(serverSource.resolve("pom.xml"))) {
            throw new IllegalStateException("The selected server source is not a maven project");
        }
        // Validate the pom.xml file
        validateServerSourcePom(serverSource);

        // Check if the server source contains the src/main/liberty/config/server.xml file
        if (!Files.exists(serverSource.resolve("src/main/liberty/config/server.xml"))) {
            throw new IllegalStateException("The selected server source does not contain the server.xml file");
        }

        // Check if the server contains the maven wrapper for the current OS
        if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
            if (!Files.exists(serverSource.resolve("mvnw.cmd"))) {
                throw new IllegalStateException("The selected server source does not contain the maven wrapper for Windows");
            }
        } else { // Linux or Mac
            if (!Files.exists(serverSource.resolve("mvnw"))) {
                throw new IllegalStateException("The selected server source does not contain the maven wrapper for Linux or Mac");
            }
        }

        // Check if the server source is a git repository
        // Check if the selected directory is a Git repository and fetch the latest commit hash and message
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("git", "rev-parse", "--is-inside-work-tree");
        processBuilder.directory(serverSource.toFile());

        try {
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if ("true".equals(line)) {
                    // It's a git repository, print the latest commit hash and message
                    processBuilder.command("git", "log", "-1", "--pretty=format:%h - %s");
                    process = processBuilder.start();
                    reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    while ((line = reader.readLine()) != null) {
                        terminal.writer().println(
                                new AttributedStringBuilder()
                                        .append("Latest commit: " + line + "\n", AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                                        .toAnsi()
                        );
                    }
                } else {
                    terminal.writer().println(
                            new AttributedStringBuilder()
                                    .append("Not a Git repository\n", AttributedStyle.DEFAULT.foreground(AttributedStyle.RED))
                                    .toAnsi()
                    );
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error while checking Git repository", e);
        }
    }

    private static void validateServerSourcePom(Path serverSource) {
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
                throw new IllegalStateException("The pom.xml file does not contain the liberty-maven-plugin");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read the pom.xml file", e);
        }
    }

    private static Path pickServerSource(Terminal terminal) {
        // Create a writer
        PrintWriter writer = terminal.writer();


        // Create a LineReader
        LineReader lineReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();

        // Path of the directory
        String currentWorkingDirectory = System.getProperty("user.dir");
        Path directory = Paths.get(currentWorkingDirectory, "server-sources");

        // Create a string builder
        AttributedStringBuilder asb = new AttributedStringBuilder();
        // Append the location of the server sources
        asb.append("Looking for server sources in: ");
        asb.append(directory.toString(), AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
        // Print the message
        writer.println(asb.toAnsi());

        // Get subfolders
        List<String> subfolders;
        try {
            subfolders = Files.walk(directory, 1)
                    .filter(Files::isDirectory)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read server-sources", e);
        }

        // Remove the first element because it's the parent directory itself
        subfolders = subfolders.subList(1, subfolders.size());

        // If there are no subfolders, write a message and exit
        if (subfolders.isEmpty()) {
            writer.println(
                    new AttributedStringBuilder()
                            .append("No server sources found\n", AttributedStyle.DEFAULT.foreground(AttributedStyle.RED))
                            .toAnsi()
            );
            System.exit(1);
        }
        // If there is only one subfolder, use it
        if (subfolders.size() == 1) {
            // Inform the user that the only subfolder will be used and which one it is
            writer.println(
                    new AttributedStringBuilder()
                            .append("Using the only server source found: ", AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                            .append(subfolders.get(0) + "\n", AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
                            .toAnsi()
            );

            return directory.resolve(subfolders.get(0));
        }


        // Display the list of subfolders with color
        terminal.writer().println(
                new AttributedStringBuilder()
                        .append("Select a subfolder:\n", AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                        .toAnsi()
        );
        for (int i = 0; i < subfolders.size(); i++) {
            terminal.writer().println(
                    new AttributedStringBuilder()
                            .append((i + 1) + ". " + subfolders.get(i) + "\n", AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE))
                            .toAnsi()
            );
        }

        String selectedFolder;
        while (true) {
            String input = lineReader.readLine("> ");

            try {
                int selection = Integer.parseInt(input);
                if (selection > 0 && selection <= subfolders.size()) {
                    selectedFolder = subfolders.get(selection - 1);
                    break;
                } else {
                    terminal.writer().println(
                            new AttributedStringBuilder()
                                    .append("Invalid selection, try again.\n", AttributedStyle.DEFAULT.foreground(AttributedStyle.RED))
                                    .toAnsi()
                    );
                }
            } catch (NumberFormatException e) {
                terminal.writer().println(
                        new AttributedStringBuilder()
                                .append("Invalid input, try again.\n", AttributedStyle.DEFAULT.foreground(AttributedStyle.RED))
                                .toAnsi()
                );
            }
        }

        terminal.writer().println(
                new AttributedStringBuilder()
                        .append("You selected: " + selectedFolder + "\n", AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                        .toAnsi()
        );

        return directory.resolve(selectedFolder);
    }


    public static Process startMavenProcess(Path serverSource, Path mavenLogFile, Terminal terminal) throws IOException {
        // Create a writer
        PrintWriter writer = terminal.writer();

        // Create a string builder
        AttributedStringBuilder asb = new AttributedStringBuilder();
        // Append that we are going to try to start the server
        asb.append("Attempting to start the server...\n", AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
        // Print the message
        writer.println(asb.toAnsi());

        // Define the Maven command
        String[] mavenCommand;
        if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
            mavenCommand = new String[]{"cmd.exe", "/c", "mvn liberty:run"};
        } else { // Linux or Mac
            mavenCommand = new String[]{"/bin/bash", "-c", "mvn liberty:run"};
        }

        // Define the working directory
        Path workingDirectoryPath = serverSource;
        File workingDirectory = workingDirectoryPath.toFile();

        // Start the Maven process
        ProcessBuilder processBuilder = new ProcessBuilder(mavenCommand);
        processBuilder.directory(workingDirectory);
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(mavenLogFile.toFile()); // log file


        return processBuilder.start();
    }

    private static void monitorLogFile(File logFile) {
        long lastReadPosition = 0;
        while (!serverReady) {
            try {
                RandomAccessFile logFileReader = new RandomAccessFile(logFile, "r");
                logFileReader.seek(lastReadPosition);
                String logLine;
                while ((logLine = logFileReader.readLine()) != null) {
                    if (logLine.contains("[INFO] [AUDIT   ] CWWKF0011I:") && logLine.contains("The defaultServer server is ready to run a smarter planet")) {
                        serverReady = true;
                        break;
                    }
                }
                lastReadPosition = logFileReader.getFilePointer();
                logFileReader.close();
                if (!serverReady) {
                    TimeUnit.SECONDS.sleep(1); // Wait a bit before next read attempt to not over-utilize CPU
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

/*
    public static void startREPL(Terminal terminal) throws IOException {
        // Create a line reader
        LineReader lineReader = LineReaderBuilder.builder().terminal(terminal).parser(new DefaultParser())
                .variable(LineReader.HISTORY_FILE, Paths.get(System.getProperty("user.home"), ".myapp_history"))
                .build();

        String line;
        while (true) {
            line = lineReader.readLine("Enter command (start, stop, status, exit): ");
            switch (line.trim()) {
                case "start":
                    mockDiscoveryService.startServer();
                    break;
                case "stop":
                    mockDiscoveryService.stopServer();
                    break;
                case "status":
                    mockDiscoveryService.status();
                    break;
                case "exit":
                    mockDiscoveryService.stopServer();
                    System.exit(0);
                default:
                    System.out.println("Invalid command. Please enter start, stop, status, or exit.");
                    break;
            }
        }
    }
*/
}