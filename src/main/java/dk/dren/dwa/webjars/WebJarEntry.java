package dk.dren.dwa.webjars;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;

import java.io.Reader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * An entry from a web jar
 */
@RequiredArgsConstructor
@Getter
public class WebJarEntry {
    private final String artifactName;
    private final String fileName;
    private final String version;
    private final String path;

    public InputStream getInputStream() throws IOException {
        final InputStream result = WebJarEntry.class.getClassLoader().getResourceAsStream(path);
        if (result == null) {
            throw new IOException("Cannot read " + path + " from classpath");
        }
        return result;
    }

    public Reader getReader() throws IOException {
        return new InputStreamReader(getInputStream());
    }

    public String getContent() throws IOException {
        return IOUtils.toString(getReader());
    }
}
