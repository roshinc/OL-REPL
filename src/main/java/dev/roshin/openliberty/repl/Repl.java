package dev.roshin.openliberty.repl;

import dev.roshin.openliberty.repl.controllers.jmx.JMXServerManager;
import dev.roshin.openliberty.repl.controllers.maven.OpenLibertyMavenWrapper;
import dev.roshin.openliberty.repl.controllers.shell.OpenLibertyServerScriptWrapper;
import dev.roshin.openliberty.repl.util.StartStopUtil;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Repl {

    private final File serverSourceRunningFile;
    private final Path serverSource;
    private final Path logFile;
    private final Path libertyPluginFile;
    private final Terminal terminal;
    private final JMXServerManager jmxServerManager;
    private final OpenLibertyMavenWrapper openLibertyMavenWrapper;
    private final OpenLibertyServerScriptWrapper openLibertyServerScriptWrapper;
    private final Logger logger;

    public Repl(File serverSourceRunningFile, Path logFile, Path libertyPluginFile, OpenLibertyMavenWrapper openLibertyMavenWrapper, OpenLibertyServerScriptWrapper openLibertyServerScriptWrapper, JMXServerManager jmxServerManager, Terminal terminal) {
        this.serverSourceRunningFile = serverSourceRunningFile;
        this.serverSource = serverSourceRunningFile.toPath().getParent();
        this.logFile = logFile;
        this.libertyPluginFile = libertyPluginFile;
        this.terminal = terminal;

        this.openLibertyMavenWrapper = openLibertyMavenWrapper;
        this.openLibertyServerScriptWrapper = openLibertyServerScriptWrapper;
        this.jmxServerManager = jmxServerManager;

        this.logger = LoggerFactory.getLogger(getClass());
    }

    public void start() throws Exception {
        logger.info("Starting REPL");
        // Create a line reader
        LineReader lineReader = LineReaderBuilder.builder().terminal(terminal).parser(new DefaultParser())
                .variable(LineReader.HISTORY_FILE, Paths.get(System.getProperty("user.home"), ".myapp_history"))
                .build();

        String line;
        while (true) {
            line = lineReader.readLine("Enter command (start, stop, status, exit): ");
            switch (line.trim()) {
                case "start":
                    // If the server is already running, do not start it again
                    if (openLibertyServerScriptWrapper.isTheServerRunning()) {
                        terminal.writer().println("Server is already running");
                        break;
                    }
                    Process mavenProcess = null;
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
                    return;
                case "stop":
                    StartStopUtil.stopServer(openLibertyServerScriptWrapper, openLibertyMavenWrapper, terminal);
                    // Delete the running file
                    if (serverSourceRunningFile.exists()) {
                        serverSourceRunningFile.delete();
                    }
                    break;
                case "status":
                    if (openLibertyServerScriptWrapper.isTheServerRunning()) {
                        terminal.writer().println(jmxServerManager.getServerInfo().toTerminalString());
                        terminal.writer().println(openLibertyServerScriptWrapper.version());
                    } else {
                        terminal.writer().println("Server is not running");
                    }
                    break;
                case "exit":
                    StartStopUtil.stopServer(openLibertyServerScriptWrapper, openLibertyMavenWrapper, terminal);
                    // Delete the running file
                    if (serverSourceRunningFile.exists()) {
                        serverSourceRunningFile.delete();
                    }
                    return;
                default:
                    System.out.println("Invalid command. Please enter start, stop, status, or exit.");
                    break;
            }
        }
    }
}
