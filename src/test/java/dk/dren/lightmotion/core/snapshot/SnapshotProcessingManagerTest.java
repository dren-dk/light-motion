package dk.dren.lightmotion.core.snapshot;

import lombok.extern.java.Log;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Log
public class SnapshotProcessingManagerTest {



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

        String zipName = "cam0";

        try (ZipInputStream zis = new ZipInputStream(SnapshotProcessingManager.class.getResourceAsStream("/"+zipName+".zip"))) {

            while (true) {
                ZipEntry entry = zis.getNextEntry();
                if (entry == null) {
                    break;
                }
                if (!entry.isDirectory() && entry.getName().endsWith(".png")) {
                    log.info("Processing "+entry.getName());
                    String name = zipName + "-" + entry.getName().substring(0, entry.getName().length() - ".png".length());
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