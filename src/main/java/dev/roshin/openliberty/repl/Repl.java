package dev.roshin.openliberty.repl;

import dev.roshin.openliberty.repl.controllers.jmx.JMXServerManager;
import dev.roshin.openliberty.repl.controllers.jmx.JMXServerManagerImpl;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;

public class Repl {

    private final File serverSourceRunningFile;
    private final Terminal terminal;
    private final Logger logger;

    private final JMXServerManager jmxRestConnector;

    public Repl(URL jmxRestConnectorURL, File serverSourceRunningFile, String username, String password, Terminal terminal) throws Exception {
        this.serverSourceRunningFile = serverSourceRunningFile;
        this.terminal = terminal;
        this.logger = LoggerFactory.getLogger(getClass());


        this.jmxRestConnector = new JMXServerManagerImpl(jmxRestConnectorURL, username, password);
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
                    //jmxRestConnector.startServer();
                    break;
                case "stop":
                    jmxRestConnector.stopServer();
                    // Delete the running file
                    if (serverSourceRunningFile.exists()) {
                        serverSourceRunningFile.delete();
                    }
                    break;
                case "status":
                    terminal.writer().println(jmxRestConnector.getServerInfo().toTerminalString());
                    break;
                case "exit":
                    jmxRestConnector.stopServer();
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
