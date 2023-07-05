package dev.roshin.openliberty.repl.controllers.maven;

import org.jline.terminal.impl.ExternalTerminal;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

class OpenLibertyMavenWrapperTest {
    private static OpenLibertyMavenWrapper openLibertyMavenWrapper;

    @BeforeAll
    static void beforeAll() throws IOException {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream outIn = new PipedOutputStream(in);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // Create a terminal
        ExternalTerminal console = new ExternalTerminal("foo", "ansi", in, out, StandardCharsets.UTF_8);

        Path pathToThServerSource = Paths.get("server-sources", "start-stop-openliberty-server-programatically");
        openLibertyMavenWrapper = new OpenLibertyMavenWrapper(pathToThServerSource, Paths.get("test-logs"), console);
    }

    @Test
    void startServerMavenProcess() throws IOException {
        openLibertyMavenWrapper.startServerMavenProcess();
    }

    @Test
    void stopServerMavenProcess() throws IOException {
        openLibertyMavenWrapper.stopServerMavenProcess();
    }
}