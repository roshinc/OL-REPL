package dev.roshin.openliberty.repl.controllers.maven.domain;

import java.io.File;

public class MavenAndLogFileResponse {
    private Process mavenProcess;
    private File logFilePath;

    public MavenAndLogFileResponse(Process mavenProcess, File logFilePath) {
        this.mavenProcess = mavenProcess;
        this.logFilePath = logFilePath;
    }

    public Process getMavenProcess() {
        return mavenProcess;
    }

    public File getLogFile() {
        return logFilePath;
    }
}
