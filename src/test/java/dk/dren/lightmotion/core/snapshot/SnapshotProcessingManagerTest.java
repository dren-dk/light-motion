package dk.dren.lightmotion.core.snapshot;

import dk.dren.lightmotion.core.events.LightMotionEventConsumer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class SnapshotProcessingManagerTest {



    @Test
    public void detectMovement() throws IOException {

        LoggingEventConsumer loggingEventConsumer = new LoggingEventConsumer();
        File workingDir = File.createTempFile("test", ".dir");
        FileUtils.forceDelete(workingDir);
        FileUtils.forceMkdir(workingDir);
        SnapshotProcessingManager spm = new SnapshotProcessingManager("test", workingDir, false, loggingEventConsumer);
        try (ZipInputStream zis = new ZipInputStream(SnapshotProcessingManager.class.getResourceAsStream("/car-leaving.zip"))) {

            while (true) {
                ZipEntry entry = zis.getNextEntry();
                if (entry == null) {
                    break;
                }
                if (!entry.isDirectory() && entry.getName().endsWith(".png")) {
                    loggingEventConsumer.setPrefix(entry.getName()+" ");
                    byte[] data = IOUtils.toByteArray(zis);
                    spm.processSnapshot(data);
                }
                zis.closeEntry();
            }


        }
    }

}