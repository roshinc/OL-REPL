package dev.roshin.openliberty.repl.controllers.shell;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import dev.roshin.openliberty.repl.config.exceptions.ConfigurationReaderException;
import dev.roshin.openliberty.repl.config.generated.LibertyPluginConfigs;
import dev.roshin.openliberty.repl.controllers.shell.exceptions.OpenLibertyScriptExecutionException;
import dev.roshin.openliberty.repl.controllers.utils.ProcessUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * This class provides a Java wrapper around the Open Liberty server shell/batch script.
 * It allows to execute the script commands from Java code.
 *
 * @see <a href="https://openliberty.io/docs/latest/reference/command/server-commands.html">Open Liberty commands</a>
 */
public class OpenLibertyServerScriptWrapper {

    private File binPath;
    private String serverName;
    private final boolean isWindows;
    private final Path logPath;
    private final Duration timeout;

    private final LibertyPluginConfigs libertyPluginConfig;

    private final Logger logger;

    public OpenLibertyServerScriptWrapper(final LibertyPluginConfigs libertyPluginConfig, final Path logPath, final Duration timeout) throws IOException {
        Preconditions.checkNotNull(libertyPluginConfig, "Liberty plugin configuration must not be null");
        Preconditions.checkNotNull(logPath, "Log path must not be null");
        Preconditions.checkArgument(!timeout.isNegative() && !timeout.isZero(), "Timeout must be greater than 0");


        this.logPath = logPath.resolve("server_script_logs");
        // Create the directories
        Files.createDirectories(this.logPath);
        this.timeout = timeout;
        this.logger = LoggerFactory.getLogger(getClass());

        this.libertyPluginConfig = libertyPluginConfig;
        loadLibertyPluginConfig(libertyPluginConfig);

        String os = System.getProperty("os.name").toLowerCase();
        logger.debug("Creating {}} for OS: {}", getClass(), os);
        isWindows = os.contains("win");
    }

    /**
     * Loads the configuration from the {@link LibertyPluginConfigs} object.
     *
     * @param libertyPluginConfig The configuration object.
     */
    private void loadLibertyPluginConfig(final LibertyPluginConfigs libertyPluginConfig) {
        this.serverName = libertyPluginConfig.getServerName();
        logger.debug("Server name: {}", serverName);

        // The server script is located in the bin directory of the Open Liberty installation directory
        this.binPath = libertyPluginConfig.getInstallDirectory().resolve("bin").toFile();
        logger.debug("Server script bin path: {}", binPath);
    }

    /**
     * Reloads the configuration from the {@link LibertyPluginConfigs} object, after invoking reload on the object.
     *
     * @throws ConfigurationReaderException If the configuration cannot be reloaded.
     */
    public void reloadLibertyPluginConfig() throws ConfigurationReaderException {
        this.libertyPluginConfig.reload();
        loadLibertyPluginConfig(this.libertyPluginConfig);
    }

    private String runCommand(final String command) throws IOException, OpenLibertyScriptExecutionException {
        logger.debug("Running command: {}", command);

        Preconditions.checkArgument(!Strings.isNullOrEmpty(command), "Command must not be null or empty");

        logger.debug("Enforcing log file limit");
        String logFilePrefix = ProcessUtils.createLogNamePrefix(command);
        ProcessUtils.enforceLogFileLimit(logFilePrefix, logPath, 5);

        ProcessBuilder processBuilder;
        String scriptName;
        if (isWindows) {
            scriptName = "server.bat ";
        } else {
            scriptName = "./server";
        }

        String timestamp = String.valueOf(System.currentTimeMillis());
        File logFile = logPath.resolve(logFilePrefix + "_" + timestamp + ".log").toFile();

        processBuilder = ProcessUtils.createProcessBuilder(scriptName, command, isWindows, binPath, logFile);
        logger.debug("Starting process");
        Process process = processBuilder.start();

        try {
            logger.debug("Waiting for process with timeout: {}s", timeout.getSeconds());
            boolean finished = process.waitFor(timeout.getSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroy();
                logger.error("Command execution timed out");
                throw new OpenLibertyScriptExecutionException("Command execution timed out");
            }
            int exitCode = process.exitValue();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    //If the line is empty, skip it
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    output.append(line.trim().strip()).append("\n");
                }
            }
            // Remove the last newline character
            output.deleteCharAt(output.length() - 1);

            String outputString = output.toString();

            if (exitCode != 0) {
                logger.error("Command execution failed with exit code {} with message {}", exitCode, outputString);
                throw new OpenLibertyScriptExecutionException(
                        String.format("Command execution failed with exit code: %s and message: %s", exitCode,
                                outputString)
                );
            }

            return outputString;

        } catch (InterruptedException e) {
            logger.error("Command execution interrupted", e);
            throw new OpenLibertyScriptExecutionException("Command execution interrupted", e);
        }
    }

    /**
     * Creates a snapshot of a server and saves the result into an archive file for further tuning and diagnosis.
     *
     * @param serverName The name of the server to be dumped.
     * @throws IOException                         If an I/O error occurs.
     * @throws OpenLibertyScriptExecutionException If the command execution fails or is interrupted.
     */
    private void dump(String serverName) throws IOException, OpenLibertyScriptExecutionException {
        runCommand("dump " + serverName);
    }

    /**
     * Creates a snapshot of the server and saves the result into an archive file for further tuning and diagnosis.
     *
     * @throws IOException                         If an I/O error occurs.
     * @throws OpenLibertyScriptExecutionException If the command execution fails or is interrupted.
     */
    private void dump() throws IOException, OpenLibertyScriptExecutionException {
        runCommand("dump " + serverName);
    }


    /**
     * Creates a snapshot of the JVM status of a server and saves the result into an archive file for further tuning and diagnosis.
     *
     * @param serverName The name of the server to be dumped.
     * @throws IOException                         If an I/O error occurs.
     * @throws OpenLibertyScriptExecutionException If the command execution fails or is interrupted.
     */
    private void javaDump(String serverName) throws IOException, OpenLibertyScriptExecutionException {
        runCommand("javadump " + serverName);
    }

    /**
     * Creates a snapshot of the JVM status of the server and saves the result into an archive file for further tuning and diagnosis.
     *
     * @throws IOException                         If an I/O error occurs.
     * @throws OpenLibertyScriptExecutionException If the command execution fails or is interrupted.
     */
    private void javaDump() throws IOException, OpenLibertyScriptExecutionException {
        runCommand("javadump " + serverName);
    }

    /**
     * Packages an Open Liberty server, its resources, and applications in a compressed file that you can store,
     * distribute, or deploy to a different location.
     *
     * @param serverName The name of the server to be packaged.
     * @throws IOException                         If an I/O error occurs.
     * @throws OpenLibertyScriptExecutionException If the command execution fails or is interrupted.
     */
    private void packageServer(String serverName) throws IOException, OpenLibertyScriptExecutionException {
        runCommand("package " + serverName);
    }

    /**
     * Packages the Open Liberty server, its resources, and applications in a compressed file that you can store,
     * distribute, or deploy to a different location.
     *
     * @throws IOException                         If an I/O error occurs.
     * @throws OpenLibertyScriptExecutionException If the command execution fails or is interrupted.
     */
    private void packageServer() throws IOException, OpenLibertyScriptExecutionException {
        runCommand("package " + serverName);
    }

    /**
     * Pauses all inbound work for an Open Liberty server.
     *
     * @param serverName The name of the server to be paused.
     * @throws IOException                         If an I/O error occurs.
     * @throws OpenLibertyScriptExecutionException If the command execution fails or is interrupted.
     */
    private void pause(String serverName) throws IOException, OpenLibertyScriptExecutionException {
        runCommand("pause " + serverName);
    }

    /**
     * Pauses all inbound work for the Open Liberty server.
     *
     * @throws IOException                         If an I/O error occurs.
     * @throws OpenLibertyScriptExecutionException If the command execution fails or is interrupted.
     */
    private void pause() throws IOException, OpenLibertyScriptExecutionException {
        runCommand("pause " + serverName);
    }

    /**
     * Resumes all inbound work for an Open Liberty server.
     *
     * @param serverName The name of the server to be resumed.
     * @throws IOException                         If an I/O error occurs.
     * @throws OpenLibertyScriptExecutionException If the command execution fails or is interrupted.
     */
    private void resume(String serverName) throws IOException, OpenLibertyScriptExecutionException {
        runCommand("resume " + serverName);
    }

    /**
     * Resumes all inbound work for the Open Liberty server.
     *
     * @throws IOException                         If an I/O error occurs.
     * @throws OpenLibertyScriptExecutionException If the command execution fails or is interrupted.
     */
    private void resume() throws IOException, OpenLibertyScriptExecutionException {
        runCommand("resume " + serverName);
    }


    /**
     * Starts the server named serverName as a foreground process.
     *
     * @param serverName The name of the server to be created.
     * @throws IOException                         If an I/O error occurs.
     * @throws OpenLibertyScriptExecutionException If the command execution fails or is interrupted.
     */
    private void run(String serverName) throws IOException, OpenLibertyScriptExecutionException {
        runCommand("run " + serverName);
    }

    /**
     * Starts the server as a foreground process.
     *
     * @throws IOException                         If an I/O error occurs.
     * @throws OpenLibertyScriptExecutionException If the command execution fails or is interrupted.
     */
    private void run() throws IOException, OpenLibertyScriptExecutionException {
        runCommand("run " + serverName);
    }

    /**
     * Starts the server named serverName as a background process.
     *
     * @param serverName The name of the server to be created.
     * @throws IOException                         If an I/O error occurs.
     * @throws OpenLibertyScriptExecutionException If the command execution fails or is interrupted.
     */
    private void start(String serverName) throws IOException, OpenLibertyScriptExecutionException {
        runCommand("start " + serverName);
    }

    /**
     * Starts the server as a background process.
     *
     * @throws IOException                         If an I/O error occurs.
     * @throws OpenLibertyScriptExecutionException If the command execution fails or is interrupted.
     */
    protected void start() throws IOException, OpenLibertyScriptExecutionException {
        start(serverName);
    }

    /**
     * Displays the status of the server named serverName.
     *
     * @param serverName The name of the server to be created.
     * @return The status of the server.
     * @throws IOException                         If an I/O error occurs.
     * @throws OpenLibertyScriptExecutionException If the command execution fails or is interrupted.
     */
    protected String status(String serverName) throws IOException, OpenLibertyScriptExecutionException {
        return runCommand("status " + serverName);
    }


    /**
     * Displays the status of the server.
     *
     * @return The status of the server.
     * @throws IOException                         If an I/O error occurs.
     * @throws OpenLibertyScriptExecutionException If the command execution fails or is interrupted.
     */
    protected String status() throws IOException, OpenLibertyScriptExecutionException {
        return status(serverName);
    }

    /**
     * Uses {@link #status()} to determine if the server is running.
     *
     * @return true if the server is running, false otherwise.
     * @throws IOException                         If an I/O error occurs.
     * @throws OpenLibertyScriptExecutionException If the command execution fails or is interrupted.
     */
    public boolean isTheServerRunning() throws IOException, OpenLibertyScriptExecutionException {
        try {
            String status = status(serverName);
            return status.contains("is running");
        } catch (OpenLibertyScriptExecutionException e) {
            // If the message contains "is not running", then the server is not running.
            if (e.getMessage().contains("is not running")) {
                return false;
            } else {
                throw e;
            }
        }
    }

    /**
     * Stops the server named serverName.
     *
     * @param serverName The name of the server to be created.
     * @throws IOException                         If an I/O error occurs.
     * @throws OpenLibertyScriptExecutionException If the command execution fails or is interrupted.
     */
    private void stop(String serverName) throws IOException, OpenLibertyScriptExecutionException {
        runCommand("stop " + serverName);
    }

    /**
     * Stops the server .
     *
     * @throws IOException                         If an I/O error occurs.
     * @throws OpenLibertyScriptExecutionException If the command execution fails or is interrupted.
     */
    public void stop() throws IOException, OpenLibertyScriptExecutionException {
        stop(serverName);
    }

    /**
     * Displays the version of the server named serverName.
     *
     * @param serverName The name of the server to be created.
     * @return The version of the server.
     * @throws IOException                         If an I/O error occurs.
     * @throws OpenLibertyScriptExecutionException If the command execution fails or is interrupted.
     */
    private String version(String serverName) throws IOException, OpenLibertyScriptExecutionException {
        return runCommand("version " + serverName);
    }

    /**
     * Displays the version of the server.
     *
     * @return The version of the server.
     * @throws IOException                         If an I/O error occurs.
     * @throws OpenLibertyScriptExecutionException If the command execution fails or is interrupted.
     */
    public String version() throws IOException, OpenLibertyScriptExecutionException {
        return version(serverName);
    }

}
