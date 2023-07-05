package dev.roshin.openliberty.repl.controllers.utils;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class ProcessUtils {
    private static final Logger logger = LoggerFactory.getLogger(ProcessUtils.class);

    private ProcessUtils() {
    }

    /**
     * @return true if the current OS is Windows, false otherwise
     */
    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    /**
     * Creates a log name prefix from the log name key
     *
     * @param logNameKey The log name key
     * @return The log name prefix, file name friendly
     */
    public static String createLogNamePrefix(String logNameKey) {
        Preconditions.checkNotNull(logNameKey, "logNameKey cannot be null");
        // Make log name key file name friendly
        return logNameKey.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    /**
     * Enforces the log file limit by deleting the oldest log files
     *
     * @param logNamePrefix The prefix of the log file name
     * @param logFilesPath  The path to the log files
     * @param logFileLimit  The limit of the log files
     */
    public static void enforceLogFileLimit(String logNamePrefix, Path logFilesPath, int logFileLimit) throws IOException {
        Preconditions.checkNotNull(logNamePrefix, "logNamePrefix cannot be null");
        Preconditions.checkNotNull(logFilesPath, "logFilesPath cannot be null");
        Preconditions.checkArgument(logFileLimit > 0, "logFileLimit must be greater than 0");

        logger.debug("Enforcing log file limit for log name prefix: {}", logNamePrefix);

        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(logFilesPath, logNamePrefix + "*")) {
            List<Path> logFiles = new ArrayList<>();
            for (Path path : directoryStream) {
                logFiles.add(path);
            }

            if (logFiles.size() > logFileLimit) {
                // Sort log files by modification time in ascending order
                logFiles.sort(Comparator.comparing(path -> {
                    try {
                        return Files.getLastModifiedTime(path).toMillis();
                    } catch (IOException e) {
                        logger.error("Failed to get last modified time of file: {}", path, e);
                        throw new UncheckedIOException(e);
                    }
                }));

                // Delete oldest files
                int filesToDelete = logFiles.size() - 5;
                for (int i = 0; i < filesToDelete; i++) {
                    Files.delete(logFiles.get(i));
                    logger.debug("Deleted log file: {}", logFiles.get(i));
                }
            }
        }
    }

    /**
     * Creates a process builder for the given command and command arguments
     * <p>
     * The process builder will redirect the standard output and error to the log file
     * The process builder will execute the command in the given execution path
     *
     * @param command          The command to run
     * @param commandArguments The command arguments
     * @param isWindows        Whether the current OS is Windows
     * @param executionPath    The path to execute the command in
     * @param logFile          The log file
     * @return The process builder
     */
    public static ProcessBuilder createProcessBuilder(String command, String commandArguments, boolean isWindows, File executionPath,
                                                      File logFile) {
        logger.debug("Creating process builder for command: {}, command arguments: {}, execution path: {}, log file: {}"
                , command, commandArguments, executionPath, logFile);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(command), "command cannot be null or empty");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(commandArguments), "command cannot be null or empty");
        Preconditions.checkNotNull(executionPath, "executionPath cannot be null");
        Preconditions.checkNotNull(logFile, "logFile cannot be null");

        //Split command arguments
        final String[] argsParts = commandArguments.split("\\s+");
        final List<String> commandList = new ArrayList<>();

        ProcessBuilder processBuilder;
        if (isWindows) {
            String cmdCommand = "cmd.exe";
            String commandCmdFlag = "/c";
            commandList.add(cmdCommand);
            commandList.add(commandCmdFlag);
        }
        commandList.add(command);
        commandList.addAll(Arrays.asList(argsParts));
        processBuilder = new ProcessBuilder(commandList);

        processBuilder.directory(executionPath);
        logger.debug("Process builder directory: {}", processBuilder.directory());
        logger.debug("Process builder commandArguments: {}", processBuilder.command());

        logger.debug("Log file: {}", logFile);
        processBuilder.redirectOutput(logFile);
        processBuilder.redirectError(logFile);

        return processBuilder;
    }
}
