/**
 *
 */
package dev.roshin.openliberty.repl;

import dev.roshin.openliberty.repl.controllers.jmx.rest.JMXUtil;
import dev.roshin.openliberty.repl.preparers.ServerXMLPreparer;
import dev.roshin.openliberty.repl.util.ServerSourceUtils;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class Main {

    // The maven process
    private static Process mavenProcess = null;
    private static volatile boolean serverReady = false;

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException {
        logger.debug("Starting Main");
        logger.error("Checking error log");

        // Create a terminal
        Terminal terminal = TerminalBuilder.builder().system(true).build();

        // Print the introduction
        printIntroduction(terminal);

        logger.debug("Introduction printed");

        // Pick the server source
        Path serverSource = ServerSourceUtils.pickServerSource(terminal);

        logger.debug("Server source picked: {}", serverSource);

        //TODO: Check if server source is already running

        // Prepare the server source
        prepareServerSource(serverSource, terminal);

        // Create a file name for the log file unique to this run
        String logFileName = "maven_log_" + System.currentTimeMillis() + ".txt";
        // Store the log file in a temporary directory within the current working directory
        Path mavenLogFile = Paths.get(System.getProperty("user.dir")).resolve("temp").resolve(logFileName);
        // Create the parent directories for the log file, if they do not exist
        Files.createDirectories(mavenLogFile.getParent());

        try {
            // Start the Maven process
//            mavenProcess = OpenLibertyMavenWrapper.(serverSource, mavenLogFile, terminal);

            // Inform the user we are waiting for 30 seconds, with the word "waiting" in yellow
            AttributedStringBuilder attributedStringBuilder = new AttributedStringBuilder();
            attributedStringBuilder.append("Waiting standard ");
            attributedStringBuilder.append(String.valueOf(30), AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW));
            attributedStringBuilder.append(" seconds for server to become ready", AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW));
            terminal.writer().println(attributedStringBuilder.toAnsi());
            terminal.writer().flush();


            // Wait for 30 seconds
            TimeUnit.SECONDS.sleep(30);

            // Inform the user we are waiting for 30 seconds, with the word "waiting" in yellow
            AttributedStringBuilder configuredTimeoutStringBuilder = new AttributedStringBuilder();
            configuredTimeoutStringBuilder.append("Polling for configured timeout of ");
            configuredTimeoutStringBuilder.append(String.valueOf(60), AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW));
            configuredTimeoutStringBuilder.append(" seconds for server to become ready", AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW));
            terminal.writer().println(configuredTimeoutStringBuilder.toAnsi());
            terminal.writer().flush();

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

            // At this point, the server is ready

            // Create a temporary file that identifies the server source is running and controlled by this process
            File serverSourceRunningFile = new File(serverSource.toFile().getAbsolutePath() + ".running");

            // Inform the user that the server is ready, with the word "ready" in green
            AttributedStringBuilder serverReadyStringBuilder = new AttributedStringBuilder();
            serverReadyStringBuilder.append("Server ready", AttributedStyle.BOLD.foreground(AttributedStyle.GREEN));
            terminal.writer().println(serverReadyStringBuilder.toAnsi());
            terminal.writer().flush();

            // Write the jmx url to the terminal
            terminal.writer().println("JMX URL: " + JMXUtil.findRestConnectorURL(serverSource, terminal));
            terminal.writer().flush();

            Repl repl = new Repl(JMXUtil.findRestConnectorURL(serverSource, terminal), serverSourceRunningFile, "todd", "toddpassword", terminal);
            repl.start();

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error starting ol repl", e);
            //Inform the user that an error occurred, with the word "error" in red
            AttributedStringBuilder errorStringBuilder = new AttributedStringBuilder();
            errorStringBuilder.append("Error: ", AttributedStyle.BOLD.foreground(AttributedStyle.RED));
            errorStringBuilder.append(e.getMessage(), AttributedStyle.BOLD.foreground(AttributedStyle.RED));
            terminal.writer().println(errorStringBuilder.toAnsi());
        } finally {
            // Flush the terminal writer
            terminal.writer().flush();
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