package dev.roshin.openliberty.repl.controllers.maven;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import dev.roshin.openliberty.repl.controllers.maven.domain.MavenAndLogFileResponse;
import dev.roshin.openliberty.repl.controllers.utils.ProcessUtils;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;

public class OpenLibertyMavenWrapper {

    private final Path mavenLogFilePath;
    private final Path serverSource;
    private final Terminal terminal;
    private final boolean isWindows;
    private final Logger logger;


    public OpenLibertyMavenWrapper(Path serverSource, Path mavenLogFilePath, Terminal terminal) {
        Preconditions.checkNotNull(serverSource, "serverSource cannot be null");
        Preconditions.checkNotNull(mavenLogFilePath, "mavenLogFilePath cannot be null");
        Preconditions.checkNotNull(terminal, "terminal cannot be null");

        this.serverSource = serverSource;
        this.mavenLogFilePath = mavenLogFilePath;
        this.logger = LoggerFactory.getLogger(getClass());
        this.terminal = terminal;

        String os = System.getProperty("os.name").toLowerCase();
        logger.debug("Creating {}} for OS: {}", getClass(), os);
        isWindows = os.contains("win");
    }

    private MavenAndLogFileResponse doMavenProcess(String goals) throws IOException {
        logger.debug("Starting maven process for goals: {}", goals);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(goals), "goals cannot be null or empty");

        logger.debug("Enforcing log file limit");
        String logFilePrefix = ProcessUtils.createLogNamePrefix(goals);
        ProcessUtils.enforceLogFileLimit(logFilePrefix, mavenLogFilePath, 5);

        // Create a writer
        PrintWriter writer = terminal.writer();

        // Create a string builder
        AttributedStringBuilder asb = new AttributedStringBuilder();
        asb.append("Attempting to do maven command: ");
        // Append the maven goals
        asb.append(goals, AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
        asb.append("\n");
        // Print the message
        writer.println(asb.toAnsi());

        // Define the Maven command
       /* String[] mavenCommand;
        if (isWindows) {
            mavenCommand = new String[]{"cmd.exe", "/c", "mvn " + goals};
        } else { // Linux or Mac
            mavenCommand = new String[]{"/bin/bash", "-c", "mvn" + goals};
        }*/
        // Define the working directory
        File workingDirectory = serverSource.toFile();

        String mavenCommand;
        if (isWindows) {
            mavenCommand = "mvn";
        } else {
            mavenCommand = "mvn";
        }
        String timestamp = String.valueOf(System.currentTimeMillis());
        File logFile = mavenLogFilePath.resolve(logFilePrefix + "_" + timestamp + ".log").toFile();

        ProcessBuilder processBuilder = ProcessUtils.createProcessBuilder(mavenCommand, goals, isWindows, workingDirectory, logFile);
        logger.debug("Starting process");
        return new MavenAndLogFileResponse(processBuilder.start(), logFile);
    }


    public MavenAndLogFileResponse startServerMavenProcess() throws IOException {
        logger.debug("Starting the start server maven process");
        // Create a writer
        PrintWriter writer = terminal.writer();

        // Create a string builder
        AttributedStringBuilder asb = new AttributedStringBuilder();
        // Append that we are going to try to start the server
        asb.append("Attempting to start the server...\n", AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));

        return doMavenProcess("liberty:run");
    }


    public MavenAndLogFileResponse stopServerMavenProcess() throws IOException {
        logger.debug("Starting the stop server maven process");
        // Create a writer
        PrintWriter writer = terminal.writer();

        // Create a string builder
        AttributedStringBuilder asb = new AttributedStringBuilder();
        // Append that we are going to try to start the server
        asb.append("Attempting to stop the server...\n", AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));

        return doMavenProcess("liberty:stop");
    }
}
