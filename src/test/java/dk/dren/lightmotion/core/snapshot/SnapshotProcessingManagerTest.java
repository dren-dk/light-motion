package dk.dren.lightmotion.core.snapshot;

import lombok.extern.java.Log;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Log
public class SnapshotProcessingManagerTest {


    private static final Pattern IMAGE_NAME = Pattern.compile("(.+)\\.(png|jpe?g)");

    @Test
    public void detectMovement() throws IOException {

        LoggingEventConsumer loggingEventConsumer = new LoggingEventConsumer();
        File workingDir = File.createTempFile("test", ".dir");
        FileUtils.forceDelete(workingDir);
        FileUtils.forceMkdir(workingDir);
        try (InputStream is = SnapshotProcessingManager.class.getResourceAsStream("/clock-mask.png");
             OutputStream os = new FileOutputStream(new File(workingDir,"movement-mask.png"))) {
            IOUtils.copy(is, os);
        }

        SnapshotProcessingManager spm = new SnapshotProcessingManager("test", workingDir, false, loggingEventConsumer);

        String zipName = "cam3-night-rain";

        try (ZipInputStream zis = new ZipInputStream(SnapshotProcessingManager.class.getResourceAsStream("/"+zipName+".zip"))) {

            while (true) {
                ZipEntry entry = zis.getNextEntry();
                if (entry == null) {
                    break;
                }
                Matcher nameMatch = IMAGE_NAME.matcher(entry.getName());
                if (!entry.isDirectory() && nameMatch.matches()) {
                    log.info("Processing "+entry.getName());
                    String name = zipName + "-" + nameMatch.group(1);
                    loggingEventConsumer.setPrefix(name+" ");
                    byte[] data = IOUtils.toByteArray(zis);
                    spm.processSnapshot(name, data);
                }
                zis.closeEntry();
            }
        } finally {
            FileUtils.forceDelete(workingDir);
        }
    }

}