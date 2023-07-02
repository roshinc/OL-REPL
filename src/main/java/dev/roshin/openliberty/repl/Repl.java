package dev.roshin.openliberty.repl;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.nio.file.Paths;

public class Repl {

    private URL jmxRestConnectorURL;
    private Terminal terminal;
    private Logger logger;

    public Repl(URL jmxRestConnectorURL, Terminal terminal) {
        this.jmxRestConnectorURL = jmxRestConnectorURL;
        this.terminal = terminal;
        this.logger = LoggerFactory.getLogger(getClass());
    }

    public void start() {
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
                    //jmxRestConnector.stopServer();
                    break;
                case "status":
                    //jmxRestConnector.status();
                    break;
                case "exit":
                    //jmxRestConnector.stopServer();
                    return;
                default:
                    System.out.println("Invalid command. Please enter start, stop, status, or exit.");
                    break;
            }
        }
    }
}
