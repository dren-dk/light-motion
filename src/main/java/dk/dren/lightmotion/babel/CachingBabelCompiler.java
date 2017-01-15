package dk.dren.lightmotion.babel;

import dk.dren.lightmotion.cache.DiskCache;
import dk.dren.lightmotion.webjars.WebJarEntry;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * A babel compiler that caches the compiler output in memory and on disk to avoid having to call the compiler.
 */
public class CachingBabelCompiler {
    private final BabelCompiler compiler;
    private final DiskCache cache;
    private final Map<String, String> resourceHashes = new HashMap<>();
    private final Map<String, String> fileHashes = new HashMap<>();

    public CachingBabelCompiler(BabelCompiler compiler, DiskCache cache) {
        this.compiler = compiler;
        this.cache = cache;
    }

    public String compileWebJar(WebJarEntry entry) throws IOException {
        String desc = "artifact="+entry.getArtifactName()+" version="+entry.getVersion()+" file="+entry.getFileName();
        String key = DigestUtils.sha256Hex(desc);
        try {
            return cache.get(key, desc, () -> compiler.compile(entry.getContent()));
        } catch (Exception e) {
            throw new IOException("Failed while compiling "+desc, e);
        }
    }

    public String compileResource(String path) throws IOException {
        String key = resourceHashes.get(path);
        final String content;

        if (key == null) {
            try (InputStream contentStream = CachingBabelCompiler.class.getClassLoader().getResourceAsStream(path)) {
                if (contentStream == null) {
                    throw new FileNotFoundException("Could not find " + path + " in the classpath");
                }
                content = IOUtils.toString(contentStream, "UTF-8");
            }
            key = DigestUtils.sha256Hex(content);
            resourceHashes.put(path, key);
        } else {
            content = null;
        }

        try {
            return cache.get(key, "classpath:"+path, () -> {
                if (content == null) {
                    try (InputStream contentStream = CachingBabelCompiler.class.getClassLoader().getResourceAsStream(path)) {
                        if (contentStream == null) {
                            throw new FileNotFoundException("Could not find " + path + " in the classpath");
                        }
                        return compiler.compile(IOUtils.toString(contentStream, "UTF-8"));
                    }
                } else {
                    return compiler.compile(content);
                }
            });
        } catch (Exception e) {
            throw new IOException("Failed while compiling classpath:"+path, e);
        }
    }

    public String compileFile(File file) throws IOException {
        String hashCacheKey = file.getAbsolutePath()+":"+file.length()+":"+file.lastModified();
        String key = fileHashes.get(hashCacheKey);
        final String content;
        if (key == null) {
            try (InputStream contentStream = new FileInputStream(file)) {
                content = IOUtils.toString(contentStream, "UTF-8");
            }
            key = DigestUtils.sha256Hex(content);
            fileHashes.put(hashCacheKey, key);
        } else {
            content = null;
        }

        try {
            return cache.get(key, "file:"+file, () -> {
                if (content == null) {
                    try (InputStream contentStream = new FileInputStream(file)) {
                        return compiler.compile(IOUtils.toString(contentStream, "UTF-8"));
                    }
                } else {
                    return compiler.compile(content);
                }
            });
        } catch (Exception e) {
            throw new IOException("Failed while compiling file:"+file, e);
        }
    }

}
