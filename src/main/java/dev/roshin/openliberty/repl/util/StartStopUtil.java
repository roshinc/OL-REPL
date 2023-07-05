package dev.roshin.openliberty.repl.util;

import com.google.common.base.Preconditions;
import dev.roshin.openliberty.repl.Repl;
import dev.roshin.openliberty.repl.config.generated.LibertyPluginConfigs;
import dev.roshin.openliberty.repl.controllers.jmx.JMXServerManager;
import dev.roshin.openliberty.repl.controllers.jmx.JMXServerManagerImpl;
import dev.roshin.openliberty.repl.controllers.jmx.rest.JMXUtil;
import dev.roshin.openliberty.repl.controllers.maven.OpenLibertyMavenWrapper;
import dev.roshin.openliberty.repl.controllers.maven.domain.MavenAndLogFileResponse;
import dev.roshin.openliberty.repl.controllers.shell.OpenLibertyServerScriptWrapper;
import dev.roshin.openliberty.repl.controllers.shell.exceptions.OpenLibertyScriptExecutionException;
import org.jline.terminal.Terminal;
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
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class StartStopUtil {
    private static final Logger logger = LoggerFactory.getLogger(StartStopUtil.class);
    private static volatile boolean serverReady = false;

    private StartStopUtil() {
    }

    /**
     * Tries to stop the server first by using the shell script wrapper, then by using Maven.
     *
     * @param openLibertyServerScriptWrapper The shell script wrapper
     * @param openLibertyMavenWrapper        The Maven wrapper
     * @param terminal                       The terminal
     * @throws IOException                         If an I/O error occurs
     * @throws OpenLibertyScriptExecutionException If an error occurs while executing the shell script
     */
    public static void stopServer(OpenLibertyServerScriptWrapper openLibertyServerScriptWrapper,
                                  OpenLibertyMavenWrapper openLibertyMavenWrapper, Terminal terminal)
            throws IOException, OpenLibertyScriptExecutionException {

        // Create a writer
        final PrintWriter printWriter = terminal.writer();

        // Tell the user that the server will be stopped
        AttributedStringBuilder stoppingServerStringBuilder = new AttributedStringBuilder();
        stoppingServerStringBuilder.append("Stopping the server",
                AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW));
        printWriter.println(stoppingServerStringBuilder.toAnsi());
        printWriter.flush();
        logger.debug("Stopping the server");
        // Stop the server
        openLibertyServerScriptWrapper.stop();
        // Check if the server is stopped
        if (openLibertyServerScriptWrapper.isTheServerRunning()) {
            // Inform the user that the server could not be stopped, with the word "could not" in yellow
            AttributedStringBuilder couldNotStopServerStringBuilder = new AttributedStringBuilder();
            couldNotStopServerStringBuilder.append("Could not stop the server",
                    AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW));
            printWriter.println(couldNotStopServerStringBuilder.toAnsi());
            printWriter.flush();
            logger.debug("Could not stop the server");
            // Inform the user that we are going to try another method to stop the server
            AttributedStringBuilder tryingAnotherMethodStringBuilder = new AttributedStringBuilder();
            tryingAnotherMethodStringBuilder.append("Trying another method to stop the server",
                    AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW));
            printWriter.println(tryingAnotherMethodStringBuilder.toAnsi());
            printWriter.flush();
            logger.debug("Trying another method to stop the server");
            openLibertyMavenWrapper.stopServerMavenProcess();
            // Check if the server is stopped
            if (openLibertyServerScriptWrapper.isTheServerRunning()) {
                // Inform the user that the server could not be stopped, with the word "could not" in red
                // and that they should stop the server manually
                AttributedStringBuilder couldNotStopServerManuallyStringBuilder = new AttributedStringBuilder();
                couldNotStopServerManuallyStringBuilder.append("Could not stop the server. Please stop the server manually",
                        AttributedStyle.BOLD.foreground(AttributedStyle.RED));
                printWriter.println(couldNotStopServerManuallyStringBuilder.toAnsi());
                printWriter.flush();
                logger.debug("Could not stop the server");
            } else {
                // Inform the user that the server was stopped, with the word "stopped" in green
                AttributedStringBuilder serverStoppedStringBuilder = new AttributedStringBuilder();
                serverStoppedStringBuilder.append("Server stopped",
                        AttributedStyle.BOLD.foreground(AttributedStyle.GREEN));
                printWriter.println(serverStoppedStringBuilder.toAnsi());
                printWriter.flush();
                logger.debug("Server stopped");
                serverReady = false;
            }
        } else {
            // Inform the user that the server was stopped, with the word "stopped" in green
            AttributedStringBuilder serverStoppedStringBuilder = new AttributedStringBuilder();
            serverStoppedStringBuilder.append("Server stopped",
                    AttributedStyle.BOLD.foreground(AttributedStyle.GREEN));
            printWriter.println(serverStoppedStringBuilder.toAnsi());
            printWriter.flush();
            logger.debug("Server stopped");
            serverReady = false;
        }
    }

    public static Process startServerAndRepl(Path serverSource, OpenLibertyMavenWrapper openLibertyMavenWrapper, Path libertyPluginFile,
                                             OpenLibertyServerScriptWrapper openLibertyServerScriptWrapper, Path logFile, Terminal terminal) {
        logger.debug("Starting the server");

        Preconditions.checkNotNull(serverSource, "serverSource cannot be null");
        Preconditions.checkNotNull(openLibertyMavenWrapper, "openLibertyMavenWrapper cannot be null");
        Preconditions.checkNotNull(libertyPluginFile, "libertyPluginFile cannot be null");
        Preconditions.checkNotNull(logFile, "logFile cannot be null");
        Preconditions.checkNotNull(terminal, "terminal cannot be null");
        Preconditions.checkArgument(Files.exists(serverSource), "serverSource must exist");

        // Create a writer
        final PrintWriter printWriter = terminal.writer();

        OpenLibertyServerScriptWrapper possibleOpenLibertyServerScriptWrapper = openLibertyServerScriptWrapper;

        Process mavenProcess = null;
        try {
            // Start the Maven process
            final MavenAndLogFileResponse mavenAndLogFileResponse = openLibertyMavenWrapper.startServerMavenProcess();
            mavenProcess = mavenAndLogFileResponse.getMavenProcess();

            // Inform the user we are waiting for 30 seconds, with the word "waiting" in yellow
            AttributedStringBuilder attributedStringBuilder = new AttributedStringBuilder();
            attributedStringBuilder.append("Waiting standard ");
            attributedStringBuilder.append(String.valueOf(30), AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW));
            attributedStringBuilder.append(" seconds for server to become ready", AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW));
            printWriter.println(attributedStringBuilder.toAnsi());
            printWriter.flush();


            // Wait for 30 seconds
            TimeUnit.SECONDS.sleep(30);

            // Inform the user we are waiting for 30 seconds, with the word "waiting" in yellow
            AttributedStringBuilder configuredTimeoutStringBuilder = new AttributedStringBuilder();
            configuredTimeoutStringBuilder.append("Polling for configured timeout of ");
            configuredTimeoutStringBuilder.append(String.valueOf(60), AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW));
            configuredTimeoutStringBuilder.append(" seconds for server to become ready", AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW));
            printWriter.println(configuredTimeoutStringBuilder.toAnsi());
            printWriter.flush();

            // Start a separate thread to monitor the log file
            Thread logMonitorThread = new Thread(() -> monitorLogFile(mavenAndLogFileResponse.getLogFile()));
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
            printWriter.println(serverReadyStringBuilder.toAnsi());
            printWriter.flush();

            // Check if the shell script wrapper is null
            if (possibleOpenLibertyServerScriptWrapper == null) {
                // Load the liberty plugin file
                LibertyPluginConfigs libertyPluginConfigs = new LibertyPluginConfigs(libertyPluginFile);
                // Create a new shell script wrapper
                possibleOpenLibertyServerScriptWrapper = new OpenLibertyServerScriptWrapper(libertyPluginConfigs, logFile.getParent(), Duration.ofSeconds(100));
            } else {
                possibleOpenLibertyServerScriptWrapper.reloadLibertyPluginConfig();
            }
            // Create JMX manager
            JMXServerManager jmxServerManager = new JMXServerManagerImpl(JMXUtil.findRestConnectorURL(serverSource, terminal), "todd", "toddpassword");

            logger.debug("Server is running");
            logger.debug("Shell script wrapper says server is running: " + possibleOpenLibertyServerScriptWrapper.isTheServerRunning());
            logger.debug("JMX manager can connect: " + jmxServerManager.isConnectable());

            // Start the REPL
            logger.debug("Starting the REPL");
            Repl repl = new Repl(serverSourceRunningFile, logFile, libertyPluginFile, openLibertyMavenWrapper, possibleOpenLibertyServerScriptWrapper, jmxServerManager, terminal);
            repl.start();

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error starting ol repl", e);
            //Inform the user that an error occurred, with the word "error" in red
            AttributedStringBuilder errorStringBuilder = new AttributedStringBuilder();
            errorStringBuilder.append("Error: ", AttributedStyle.BOLD.foreground(AttributedStyle.RED));
            errorStringBuilder.append(e.getMessage(), AttributedStyle.BOLD.foreground(AttributedStyle.RED));
            printWriter.println(errorStringBuilder.toAnsi());
        } finally {
            // Flush the terminal writer
            printWriter.flush();
            // If the server was not stopped in a normal way, or if an error occurred,
            // the Maven process is killed here
            if (mavenProcess != null && mavenProcess.isAlive()) {
                mavenProcess.destroyForcibly();
            }
        }
        return mavenProcess;
    }

    /**
     * Monitors the log file for the server ready message
     *
     * @param logFile The log file to monitor
     */
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
}
