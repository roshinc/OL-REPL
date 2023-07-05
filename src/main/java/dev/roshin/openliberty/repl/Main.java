/**
 *
 */
package dev.roshin.openliberty.repl;

import dev.roshin.openliberty.repl.config.exceptions.ConfigurationReaderException;
import dev.roshin.openliberty.repl.config.generated.LibertyPluginConfigs;
import dev.roshin.openliberty.repl.controllers.maven.OpenLibertyMavenWrapper;
import dev.roshin.openliberty.repl.controllers.shell.OpenLibertyServerScriptWrapper;
import dev.roshin.openliberty.repl.controllers.shell.exceptions.OpenLibertyScriptExecutionException;
import dev.roshin.openliberty.repl.controllers.utils.ProcessUtils;
import dev.roshin.openliberty.repl.preparers.ServerXMLPreparer;
import dev.roshin.openliberty.repl.util.ServerSourceUtils;
import dev.roshin.openliberty.repl.util.StartStopUtil;
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
import java.time.Duration;
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

        // Create a file name for the log file unique to this run
        String logFileName = "ol_repl_log_" + System.currentTimeMillis() + ".log";
        // Store the log file in a temporary directory within the current working directory
        Path logFile = Paths.get(System.getProperty("user.dir")).resolve("temp").resolve(logFileName);
        // Create the parent directories for the log file, if they do not exist
        Files.createDirectories(logFile.getParent());
        // Enforce the log file limit
        ProcessUtils.enforceLogFileLimit("ol_repl_log_", logFile.getParent(), 10);

        // Create a maven wrapper
        OpenLibertyMavenWrapper openLibertyMavenWrapper = new OpenLibertyMavenWrapper(serverSource, logFile.getParent(), terminal);
        // Create a shell script wrapper
        OpenLibertyServerScriptWrapper openLibertyServerScriptWrapper = null;

        // Create path to the liberty plugin file in the target directory, which may not exist yet
        Path libertyPluginFile = serverSource.resolve("target").resolve("liberty-plugin-config.xml");
        //Check if the liberty plugin file exists
        if (Files.exists(libertyPluginFile)) {
            // Load the liberty plugin file
            try {
                LibertyPluginConfigs libertyPluginConfigs = new LibertyPluginConfigs(libertyPluginFile);
                // Check if the server is running, using the shell script
                openLibertyServerScriptWrapper =
                        new OpenLibertyServerScriptWrapper(libertyPluginConfigs, logFile.getParent(), Duration.ofSeconds(100));
                if (openLibertyServerScriptWrapper.isTheServerRunning()) {
                    // Inform the user that an old instance of the server is running, with the word "old" in yellow
                    AttributedStringBuilder oldServerRunningStringBuilder = new AttributedStringBuilder();
                    oldServerRunningStringBuilder.append("Old server instance is running",
                            AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW));
                    terminal.writer().println(oldServerRunningStringBuilder.toAnsi());
                    terminal.writer().flush();
                    logger.debug("Old server instance is running");
                    StartStopUtil.stopServer(openLibertyServerScriptWrapper, openLibertyMavenWrapper, terminal);

                }
            } catch (ConfigurationReaderException | OpenLibertyScriptExecutionException e) {
                // Inform the user that the server might be in unknown state, with the word "unknown" in yellow
                AttributedStringBuilder unknownStateStringBuilder = new AttributedStringBuilder();
                unknownStateStringBuilder.append("Server might be in an unknown state, please check manually",
                        AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW));
                terminal.writer().println(unknownStateStringBuilder.toAnsi());
                terminal.writer().flush();
                logger.error("Error while reading the liberty plugin config file or executing the shell script", e);
                throw new RuntimeException(e);
            }
        }

        // Prepare the server source
        prepareServerSource(serverSource, terminal);

        try {
            mavenProcess = StartStopUtil.startServerAndRepl(serverSource, openLibertyMavenWrapper, libertyPluginFile,
                    openLibertyServerScriptWrapper, logFile, terminal);
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