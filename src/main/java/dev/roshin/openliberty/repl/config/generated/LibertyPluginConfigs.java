package dev.roshin.openliberty.repl.config.generated;

import dev.roshin.openliberty.repl.config.exceptions.ConfigurationReaderException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This class parses the configuration file for the Open Liberty Maven plugin in the target directory.
 */
public class LibertyPluginConfigs {

    private final Path filePath;
    private Document document;
    private Element rootElement;

    private Path installDirectory;
    private Path serverDirectory;
    private Path userDirectory;
    private Path serverOutputDirectory;
    private String serverName;
    private Path configDirectory;
    private Path configFile;
    private String appsDirectory;
    private boolean looseApplication;
    private boolean stripVersion;
    private String installAppPackages;
    private String applicationFilename;
    private String assemblyArtifactGroupId;
    private String assemblyArtifactArtifactId;
    private String assemblyArtifactVersion;
    private String assemblyArtifactType;
    private Path assemblyArchive;
    private Path assemblyInstallDirectory;
    private boolean refresh;
    private Path installAppsConfigDropins;
    private String projectType;

    private final Logger logger;

    public LibertyPluginConfigs(Path filePath) throws ConfigurationReaderException {
        this.logger = LoggerFactory.getLogger(getClass());
        this.filePath = filePath;
        load();
    }

    public LibertyPluginConfigs(String basePath) throws ConfigurationReaderException {
        this(Paths.get(basePath, "target", "liberty-plugin-config.xml"));
    }

    /**
     * Loads the configuration file.
     */
    private void load() throws ConfigurationReaderException {
        logger.debug("Start load");
        logger.debug("Loading configuration file: {}", filePath);

        try {
            SAXBuilder saxBuilder = new SAXBuilder();
            document = saxBuilder.build(filePath.toFile());
            rootElement = document.getRootElement();

            installDirectory = Paths.get(rootElement.getChildText("installDirectory"));
            serverDirectory = Paths.get(rootElement.getChildText("serverDirectory"));
            userDirectory = Paths.get(rootElement.getChildText("userDirectory"));
            serverOutputDirectory = Paths.get(rootElement.getChildText("serverOutputDirectory"));
            serverName = rootElement.getChildText("serverName");
            configDirectory = Paths.get(rootElement.getChildText("configDirectory"));
            configFile = Paths.get(rootElement.getChildText("configFile"));
            appsDirectory = rootElement.getChildText("appsDirectory");
            looseApplication = Boolean.parseBoolean(rootElement.getChildText("looseApplication"));
            stripVersion = Boolean.parseBoolean(rootElement.getChildText("stripVersion"));
            installAppPackages = rootElement.getChildText("installAppPackages");
            applicationFilename = rootElement.getChildText("applicationFilename");

            Element assemblyArtifact = rootElement.getChild("assemblyArtifact");
            assemblyArtifactGroupId = assemblyArtifact.getChildText("groupId");
            assemblyArtifactArtifactId = assemblyArtifact.getChildText("artifactId");
            assemblyArtifactVersion = assemblyArtifact.getChildText("version");
            assemblyArtifactType = assemblyArtifact.getChildText("type");

            assemblyArchive = Paths.get(rootElement.getChildText("assemblyArchive"));
            assemblyInstallDirectory = Paths.get(rootElement.getChildText("assemblyInstallDirectory"));
            refresh = Boolean.parseBoolean(rootElement.getChildText("refresh"));
            installAppsConfigDropins = Paths.get(rootElement.getChildText("installAppsConfigDropins"));
            projectType = rootElement.getChildText("projectType");

        } catch (JDOMException e) {
            logger.error("Error parsing XML file: {}", filePath, e);
            throw new ConfigurationReaderException("Error parsing XML file: " + filePath, e);
        } catch (IOException e) {
            logger.error("Error reading XML file: {}", filePath, e);
            throw new ConfigurationReaderException("Error reading XML file: " + filePath, e);
        } catch (Exception e) {
            logger.error("Unknown error occurred while loading configuration: {}", filePath, e);
            throw new ConfigurationReaderException("Unknown error occurred while loading configuration: " + filePath, e);
        }
    }

    /**
     * Reloads the configuration file.
     */
    public void reload() throws ConfigurationReaderException {
        load();
    }

    public Path getFilePath() {
        return filePath;
    }

    public Document getDocument() {
        return document;
    }

    public Element getRootElement() {
        return rootElement;
    }

    public Path getInstallDirectory() {
        return installDirectory;
    }

    public Path getServerDirectory() {
        return serverDirectory;
    }

    public Path getUserDirectory() {
        return userDirectory;
    }

    public Path getServerOutputDirectory() {
        return serverOutputDirectory;
    }

    public String getServerName() {
        return serverName;
    }

    public Path getConfigDirectory() {
        return configDirectory;
    }

    public Path getConfigFile() {
        return configFile;
    }

    public String getAppsDirectory() {
        return appsDirectory;
    }

    public boolean isLooseApplication() {
        return looseApplication;
    }

    public boolean isStripVersion() {
        return stripVersion;
    }

    public String getInstallAppPackages() {
        return installAppPackages;
    }

    public String getApplicationFilename() {
        return applicationFilename;
    }

    public String getAssemblyArtifactGroupId() {
        return assemblyArtifactGroupId;
    }

    public String getAssemblyArtifactArtifactId() {
        return assemblyArtifactArtifactId;
    }

    public String getAssemblyArtifactVersion() {
        return assemblyArtifactVersion;
    }

    public String getAssemblyArtifactType() {
        return assemblyArtifactType;
    }

    public Path getAssemblyArchive() {
        return assemblyArchive;
    }

    public Path getAssemblyInstallDirectory() {
        return assemblyInstallDirectory;
    }

    public boolean isRefresh() {
        return refresh;
    }

    public Path getInstallAppsConfigDropins() {
        return installAppsConfigDropins;
    }

    public String getProjectType() {
        return projectType;
    }
}

