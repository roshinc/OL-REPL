package dev.roshin.openliberty.repl.controllers.jmx;

import dev.roshin.openliberty.repl.controllers.jmx.domain.ApplicationStatus;
import dev.roshin.openliberty.repl.controllers.jmx.domain.ServerInfo;

import java.util.List;

public interface JMXServerManager {

    public boolean isConnectable();

    public ServerInfo getServerInfo();

    public boolean stopServer() throws Exception;

    public List<ApplicationStatus> getAllApplicationStatus() throws Exception;

    public void restartAllApplications() throws Exception;
}
