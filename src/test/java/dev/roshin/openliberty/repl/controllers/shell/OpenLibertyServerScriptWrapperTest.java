package dev.roshin.openliberty.repl.controllers.shell;

import com.google.common.base.Strings;
import dev.roshin.openliberty.repl.config.exceptions.ConfigurationReaderException;
import dev.roshin.openliberty.repl.config.generated.LibertyPluginConfigs;
import dev.roshin.openliberty.repl.controllers.shell.exceptions.OpenLibertyScriptExecutionException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;

class OpenLibertyServerScriptWrapperTest {
    private static OpenLibertyServerScriptWrapper openLibertyServerScriptWrapper;

    @BeforeAll
    static void beforeAll() throws ConfigurationReaderException, IOException {
        Path pathToTheXml = Paths.get("server-sources", "start-stop-openliberty-server-programatically",
                "target", "liberty-plugin-config.xml");

        //Load the configuration file
        LibertyPluginConfigs libertyPluginConfigs = new LibertyPluginConfigs(pathToTheXml);
        //Create the wrapper
        openLibertyServerScriptWrapper =
                new OpenLibertyServerScriptWrapper(libertyPluginConfigs, Paths.get("test-logs"), Duration.ofSeconds(100));
    }

    @Test
    void isTheServerRunning() throws OpenLibertyScriptExecutionException, IOException {
        System.out.println(openLibertyServerScriptWrapper.isTheServerRunning());
    }

    @Test
    void stop() throws OpenLibertyScriptExecutionException, IOException {
        if (!openLibertyServerScriptWrapper.isTheServerRunning()) {
            openLibertyServerScriptWrapper.start();
        }
        openLibertyServerScriptWrapper.stop();
        assertFalse(openLibertyServerScriptWrapper.isTheServerRunning());
    }

    @Test
    void version() throws OpenLibertyScriptExecutionException, IOException {
        String version = openLibertyServerScriptWrapper.version();
        assertFalse(Strings.isNullOrEmpty(version));
        System.out.println(version);
    }
}