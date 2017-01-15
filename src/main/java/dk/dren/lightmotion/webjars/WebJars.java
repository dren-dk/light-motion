package dk.dren.lightmotion.webjars;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Utility class for accessing members of web jars without hardcoding the version number of the webjar all over the code.
 */
public class WebJars {
    private final Map<String, String> knownVersions = new TreeMap<>();
    private String getVersion(String artifactName) throws IOException {
        final String knownVersion = knownVersions.get(artifactName);
        if (knownVersion != null) {
            return knownVersion;
        }

        final String pomProperties = "META-INF/maven/org.webjars.bower/"+artifactName+"/pom.properties";
        try (final InputStream pomPropertiesStream = WebJars.class.getClassLoader().getResourceAsStream(pomProperties)) {
            if (pomPropertiesStream == null) {
                throw new IOException("Unable to find "+pomProperties+" in classpath");
            }

            final Properties properties = new Properties();
            properties.load(pomPropertiesStream);
            final String version = properties.getProperty("version");
            if (version == null) {
                throw new IOException("Failed to finde the version property in "+pomProperties);
            }
            knownVersions.put(artifactName, version);
            return version;
        }
    }

    public WebJarEntry open(String artifactName, String fileName) throws IOException {
        final String version = getVersion(artifactName);
        final String path = "META-INF/resources/webjars/"+artifactName+"/"+version+"/"+fileName;
        return new WebJarEntry(artifactName, fileName, version, path);
    }
}
