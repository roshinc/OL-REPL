package dev.roshin.openliberty.repl.jmx;

import dev.roshin.openliberty.repl.jmx.domain.ApplicationStatus;
import dev.roshin.openliberty.repl.jmx.domain.ServerInfo;

import java.util.List;

public interface JMXServerManager {

    public boolean isConnectable() throws Exception;

    public ServerInfo getServerInfo();

    public void stopServer() throws Exception;

    public List<ApplicationStatus> getAllApplicationStatus() throws Exception;

    public ApplicationStatus getApplicationStatus(String appName);

    public void restartAllApplications() throws Exception;

    public void restartApplication(String appName);
}
