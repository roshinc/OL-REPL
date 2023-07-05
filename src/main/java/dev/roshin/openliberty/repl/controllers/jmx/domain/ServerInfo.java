package dev.roshin.openliberty.repl.controllers.jmx.domain;

import dev.roshin.openliberty.repl.TerminalPrintablePojo;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.nio.file.Path;

public class ServerInfo implements TerminalPrintablePojo {
    private String serverName;
    private String defaultHostname;
    private Path userDirectory;
    private Path installDirectory;
    private String libertyVersion;
    private String javaSpecificationVersion;
    private String javaRuntimeVersion;

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getDefaultHostname() {
        return defaultHostname;
    }

    public void setDefaultHostname(String defaultHostname) {
        this.defaultHostname = defaultHostname;
    }

    public Path getUserDirectory() {
        return userDirectory;
    }

    public void setUserDirectory(Path userDirectory) {
        this.userDirectory = userDirectory;
    }

    public Path getInstallDirectory() {
        return installDirectory;
    }

    public void setInstallDirectory(Path installDirectory) {
        this.installDirectory = installDirectory;
    }

    public String getLibertyVersion() {
        return libertyVersion;
    }

    public void setLibertyVersion(String libertyVersion) {
        this.libertyVersion = libertyVersion;
    }

    public String getJavaSpecificationVersion() {
        return javaSpecificationVersion;
    }

    public void setJavaSpecificationVersion(String javaSpecificationVersion) {
        this.javaSpecificationVersion = javaSpecificationVersion;
    }

    public String getJavaRuntimeVersion() {
        return javaRuntimeVersion;
    }

    public void setJavaRuntimeVersion(String javaRuntimeVersion) {
        this.javaRuntimeVersion = javaRuntimeVersion;
    }

    @Override
    public String toString() {
        return "ServerInfo{" +
                "serverName='" + serverName + '\'' +
                ", defaultHostname='" + defaultHostname + '\'' +
                ", userDirectory=" + userDirectory +
                ", installDirectory=" + installDirectory +
                ", libertyVersion='" + libertyVersion + '\'' +
                ", javaSpecificationVersion='" + javaSpecificationVersion + '\'' +
                ", javaRuntimeVersion='" + javaRuntimeVersion + '\'' +
                '}';
    }

    @Override
    public String toTerminalString() {
        return new AttributedStringBuilder()
                .append("\n")
                .append("serverName='").append(serverName, AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                .append('\'').append("\n")
                .append(", defaultHostname='").append(defaultHostname, AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN)).append('\'')
                .append("\n")
                .append(", userDirectory=").append(String.valueOf(userDirectory), AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                .append("\n")
                .append(", installDirectory=").append(String.valueOf(installDirectory), AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                .append("\n")
                .append(", libertyVersion='").append(libertyVersion, AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                .append('\'').append("\n")
                .append(", javaSpecificationVersion='").append(javaSpecificationVersion, AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                .append('\'').append("\n")
                .append(", javaRuntimeVersion='").append(javaRuntimeVersion, AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                .append('\'').append("\n")
                .toAnsi();
    }
}
