package dev.roshin.openliberty.repl.controllers.jmx.domain;

import dev.roshin.openliberty.repl.TerminalPrintablePojo;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

public class ApplicationStatus implements TerminalPrintablePojo {
    private String applicationName;
    private String status;


    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "ApplicationStatus{" +
                "applicationName='" + applicationName + '\'' +
                ", status='" + status + '\'' +
                '}';
    }

    @Override
    public String toTerminalString() {
        return new AttributedStringBuilder()
                .append("\n")
                .append("applicationName='").append(applicationName, AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                .append('\'').append("\n")
                .append(", status='").append(status, AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN)).append('\'')
                .append("\n")
                .toAnsi();
    }
}
