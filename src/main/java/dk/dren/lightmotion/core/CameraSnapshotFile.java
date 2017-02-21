package dk.dren.lightmotion.core;

import dk.dren.lightmotion.core.snapshot.SnapshotProcessingManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

@Getter
@RequiredArgsConstructor
public class CameraSnapshotFile implements CameraSnapshot {
    private final SnapshotProcessingManager snapshotProcessingManager;
    private final String name;
    private final File file;
    private byte[] imageBytes;

    @Override
    public byte[] getImageBytes() {
        synchronized (file) {
            if (imageBytes == null) {
                try {
                    imageBytes = FileUtils.readFileToByteArray(file);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return imageBytes;
        }
    }

    @Override
    public void processSnapshot() throws IOException {
        snapshotProcessingManager.processSnapshot(name, getImageBytes());
    }
}
