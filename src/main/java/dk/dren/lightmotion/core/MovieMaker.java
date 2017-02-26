package dk.dren.lightmotion.core;

import dk.dren.lightmotion.core.events.LightMotionEvent;
import dk.dren.lightmotion.core.events.LightMotionEventType;
import lombok.extern.java.Log;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;

/**
 * Implements the state machine that:
 * * Deletes the pre-recordings after the backlog time has passed without motion.
 * * Preserves pre-recordings while there's movement and concatenates the snippets to one large movie after the movement has ended.
 */
@Log
public class MovieMaker implements dk.dren.lightmotion.core.events.LightMotionEventSink {
    private final String name;
    private final File chunkDir;
    private final File recordingDir;
    private final Integer chunkLength;
    private final Integer chunksBeforeDetection;
    private final Integer chunksAfterDetection;
    private final LightMotionEventSink downstream;
    private final Thread workerThread;
    private final ArrayBlockingQueue<LightMotionEvent> events = new ArrayBlockingQueue<LightMotionEvent>(20);
    private boolean keepRunning = true;
    private long quietTime = 0;
    private boolean recording = false;
    private Set<LightMotionEventType> activeEvents = new TreeSet<>();
    private SortedDir sortedChunks;

    public MovieMaker(CameraManager cameraManager) {
        this(   cameraManager.getCameraConfig().getName(),
                cameraManager.getChunkDir(),
                cameraManager.getRecordingDir(),
                cameraManager.getLightMotion().getConfig().getChunkLength(),
                cameraManager.getLightMotion().getConfig().getChunksBeforeDetection(),
                cameraManager.getLightMotion().getConfig().getChunksAfterDetection(),
                cameraManager.getLightMotion()
                );
    }

    public MovieMaker(String name, File chunkDir, File recordingDir, Integer chunkLength, Integer chunksBeforeDetection, Integer chunksAfterDetection, LightMotionEventSink downstream) {
        this.name = name;
        this.chunkDir = chunkDir;
        this.recordingDir = recordingDir;
        this.chunkLength = chunkLength;
        this.chunksBeforeDetection = chunksBeforeDetection;
        this.chunksAfterDetection = chunksAfterDetection;
        this.downstream = downstream;
        sortedChunks = new SortedDir(chunkDir, ".mp4");

        workerThread = new Thread(() -> {
            while (keepRunning) {
                try {
                    work();
                } catch (Exception e) {
                    log.log(Level.SEVERE, "Failed while working", e);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {

                }
            }
        });
        workerThread.setName("Maintaining "+chunkDir+" to "+recordingDir+" traffic");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    @Override
    public void notify(LightMotionEvent event) {
        events.add(event); // Just add the events to the queue, so we don't block the sender
    }

    private void work() throws InterruptedException, IOException {

        processEvents(recording); // If we're recording, then the only thing that will stop us is an event, so we might as well block

        if (recording) {

            if (activeEvents.isEmpty() && (System.currentTimeMillis()-quietTime)/1000 > chunkLength*chunksAfterDetection) {
                stopRecording();
                recording = false;
            }

        } else {
            deleteOldChunks();
        }
    }

    private void processEvents(boolean block) throws InterruptedException {
        do {
            LightMotionEvent event = block ? events.take() : events.poll();
            if (event == null) {
                return;
            }

            if (event.isCanceling()) {
                activeEvents.remove(event.getType());

                if (activeEvents.isEmpty()) {
                    quietTime = event.getTimestamp();
                }
            } else {
                if (event.getType().isDetection()) {
                    activeEvents.add(event.getType());
                    recording = true;
                }
            }
        } while (!events.isEmpty());
    }

    private void deleteOldChunks() throws IOException {
        List<File> chunks = sortedChunks.list();

        // Remove all the chunks that we want to keep, iow, the newest ones
        int keepers = chunksBeforeDetection+1;
        while (!chunks.isEmpty() && keepers-- > 0) {
            chunks.remove(chunks.size()-1);
        }

        // Delete the rest
        chunks.forEach(File::delete);
    }

    private void stopRecording() throws IOException {
        // Simply append all existing chunks to the output file, except for the newest one that is probably not complete yet.
        List<File> chunks = sortedChunks.list();
        chunks.remove(chunks.size()-1);

        

        // Send an event that this has happened


        // Delete all the chunks we just used

    }

}
