package dk.dren.lightmotion.core;

import dk.dren.lightmotion.core.events.LightMotionEvent;
import dk.dren.lightmotion.core.events.LightMotionEventType;
import lombok.extern.java.Log;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Implements the state machine that:
 * * Deletes the pre-recordings after the backlog time has passed without motion.
 * * Preserves pre-recordings while there's movement and concatenates the snippets to one large movie after the movement has ended.
 */
@Log
public class MovieMaker implements dk.dren.lightmotion.core.events.LightMotionEventSink {
    private final String name;
    private final File chunkDir;
    private final Integer chunkLength;
    private final Integer chunksBeforeDetection;
    private final Integer chunksAfterDetection;
    private final LightMotion downstream;
    private final Thread workerThread;
    private final ArrayBlockingQueue<LightMotionEvent> events = new ArrayBlockingQueue<LightMotionEvent>(20);
    private final int quietTailLength;

    private boolean keepRunning = true;
    private long quietPeriodStart = 0;
    private long recordingStart = 0;
    private boolean recording = false;
    private Set<LightMotionEventType> activeEvents = new TreeSet<>();
    private SortedDir sortedChunks;
    private long lastTailLogMessage = 0;
    File recordingDir;

    public MovieMaker(CameraManager cameraManager) throws IOException {
        this(   cameraManager.getCameraConfig().getName(),
                cameraManager.getChunkDir(),
                cameraManager.getLightMotion().getConfig().getChunkLength(),
                cameraManager.getLightMotion().getConfig().getChunksBeforeDetection(),
                cameraManager.getLightMotion().getConfig().getChunksAfterDetection(),
                cameraManager.getLightMotion()
                );
    }

    public MovieMaker(String name, File chunkDir, Integer chunkLength, Integer chunksBeforeDetection, Integer chunksAfterDetection, LightMotion downstream) throws IOException {
        this.name = name;
        this.chunkDir = chunkDir;
        this.chunkLength = chunkLength;
        this.chunksBeforeDetection = chunksBeforeDetection;
        this.chunksAfterDetection = chunksAfterDetection;
        this.downstream = downstream;
        sortedChunks = new SortedDir(chunkDir, ".mp4");
        quietTailLength = chunkLength * chunksAfterDetection;

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

        processEvents(recording && !activeEvents.isEmpty()); // If we're recording and there are active events, then the only thing that will stop us is an event, so we might as well block

        if (recording) {
            long quietTailDuration = (System.currentTimeMillis() - quietPeriodStart) / 1000;

            if (System.currentTimeMillis()-lastTailLogMessage > 30000) {
                lastTailLogMessage=System.currentTimeMillis();
                if (activeEvents.isEmpty()) {
                    log.info(name + ": Still recording tail " + quietTailDuration);
                } else {
                    log.info(name + ": Still recording events active: " + getActiveEventsAsString());
                }
            }

            if (activeEvents.isEmpty() && quietTailDuration > quietTailLength) {
                stopRecording();
                recording = false;
            }

        } else {
            deleteOldChunks();
        }
    }

    private String getActiveEventsAsString() {
        return activeEvents.stream().map(Enum::toString).collect(Collectors.joining(" "));
    }

    private void processEvents(boolean block) throws InterruptedException {
        do {
            LightMotionEvent event = block ? events.take() : events.poll();
            if (event == null) {
                return;
            }
            log.fine(name+": Got event: "+event);

            if (event.isCanceling()) {
                log.info(name+": Removing event from active events: "+event.getType());
                activeEvents.remove(event.getType());

                if (activeEvents.isEmpty()) {
                    log.info(name+": Entering quiet time");
                    quietPeriodStart = event.getTimestamp();
                }
            } else {
                if (event.getType().isDetection()) {
                    activeEvents.add(event.getType());
                    recording = true;
                    recordingStart = System.currentTimeMillis();
                    if (!recording) {
                        downstream.notify(LightMotionEvent.start(LightMotionEventType.RECORDING, name, "Started recording"));
                    }
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

    String getTimestamp(long time) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        return sdf.format(new Date(time));
    }


    private void stopRecording() throws IOException, InterruptedException {
        // Simply append all existing chunks to the output file, except for the newest one that is probably not complete yet.
        String timestamp = getTimestamp(recordingStart);
        File outputFile = new File(recordingDir, name+"-"+ timestamp +".mp4");
        log.info(name+": Finishing recording "+outputFile);
        List<File> chunks = sortedChunks.list();
        chunks.remove(chunks.size()-1);

        concatenateChunks(chunks, outputFile);

        // Delete all the chunks we just used
        chunks.forEach(File::delete);

        // Send an event that this has happened
        downstream.notify(LightMotionEvent.end(LightMotionEventType.RECORDING, name, "Recorded "+outputFile.getName()));
    }

    private void concatenateChunks(List<File> chunks, File outputFile) throws IOException, InterruptedException {
        File outputDir = new File(outputFile.getParent(), outputFile.getName().replaceAll(".mp4",""));
        FileUtils.forceMkdir(outputDir);
        for (File chunk : chunks) {
            File target = new File(outputDir, chunk.getName());
            FileUtils.copyFile(chunk, target);
        }
    }

    private String __concatenateChunks(List<File> chunks, File outputFile) throws IOException, InterruptedException {

        File list = new File(outputFile.getParent(), outputFile.getName()+".list");
        FileUtils.writeStringToFile(list, chunks.stream()
                .map(f->"file '"+f.getAbsolutePath()+"'\n")
                .collect(Collectors.joining()),
                "UTF-8");

        ProcessBuilder ffmpeg = new ProcessBuilder("ffmpeg", "-f", "concat", "-safe", "0");
        ffmpeg.command().add("-i");
        ffmpeg.command().add(list.getAbsolutePath());
        ffmpeg.command().add("-c");
        ffmpeg.command().add("copy");
        ffmpeg.command().add(outputFile.getAbsolutePath());

        File errFile = new File(recordingDir, "ffmpeg."+outputFile.getName()+".err.log");
        ffmpeg.redirectError(errFile);
        File outFile = new File(recordingDir, "ffmpeg."+outputFile.getName()+".out.log");
        ffmpeg.redirectOutput(outFile);
        log.info("Running "+String.join(" ", ffmpeg.command()));
        int err = ffmpeg.start().waitFor();

        if (err == 0) {
            FileUtils.forceDelete(errFile);
            FileUtils.forceDelete(outFile);
            FileUtils.forceDelete(list);
            return null;
        } else {
            String res = "Result " + err + " of " + String.join(" ", ffmpeg.command());
            log.warning("Failed while concatenating chunks: "+err);
            return res+"\n"+FileUtils.readFileToString(outFile, "UTF-8")+"\n"+FileUtils.readFileToString(errFile, "UTF-8")+"\nEOM\n";
        }
    }


    private static final Pattern MP4BOX_CORRUPTION = Pattern.compile("Error importing ([^:]+): Corrupted Data in file/strea");
    private static final Pattern MP4BOX_NO_SUITABLE = Pattern.compile("No suitable media tracks to cat in ([^ ]+) - skipping");

    private void _concatenateChunks(List<File> chunks, File outputFile) throws IOException, InterruptedException {

        while (true) {

            if (chunks.isEmpty()) {
                log.severe("No good chunks left, giving up entirely");
                return;
            }
            String err = falliableConcatenateChunks(chunks, outputFile);
            if (err == null) {
                return;
            }

            {
                Matcher corruption = MP4BOX_CORRUPTION.matcher(err);
                if (corruption.find()) {
                    String corrupted = corruption.group(1);
                    log.warning("MP4 box detected corruption of " + corrupted + " going to leave it out and try again with the remaining chunks");
                    chunks = filterChunks(chunks, corrupted);
                    continue;
                }
            }

            {
                Matcher unSuitable = MP4BOX_NO_SUITABLE.matcher(err);
                if (unSuitable.find()) {
                    String unsuited = unSuitable.group(1);
                    log.warning("MP4 box detected unsuitable input file " + unsuited + " going to leave it out and try again with the remaining chunks");
                    chunks = filterChunks(chunks, unsuited);
                    continue;
                }
            }

            // No fixes could be found by analyzing the MP4Box output, so give up
            throw new IOException("Failed to find a fix for the problem reported by MP4Box: "+err);
        }
    }

    private static List<File> filterChunks(List<File> chunks, String goner) {
        List<File> filtered = chunks.stream().filter(f -> f.getAbsolutePath().equals(goner)).collect(Collectors.toList());
        if (filtered.size() == chunks.size()) {
            throw new RuntimeException("Failed to find "+goner);
        }
        return filtered;
    }

    private String falliableConcatenateChunks(List<File> chunks, File outputFile) throws IOException, InterruptedException {
        ProcessBuilder mp4box = new ProcessBuilder();
        mp4box.command().add("MP4Box");
        mp4box.command().add("-noprog");
        boolean first = true;
        for (File chunk : chunks) {
            if (first) {
                mp4box.command().add("-add");
                first = false;
            } else {
                mp4box.command().add("-cat");
            }
            mp4box.command().add(chunk.getAbsolutePath());
        }

        mp4box.command().add("-new");
        mp4box.command().add(outputFile.getAbsolutePath());
        File errFile = new File(recordingDir, "mp4box."+outputFile.getName()+".err.log");
        mp4box.redirectError(errFile);
        File outFile = new File(recordingDir, "mp4box."+outputFile.getName()+".out.log");
        mp4box.redirectOutput(outFile);
        log.info("Running "+String.join(" ", mp4box.command()));
        int err = mp4box.start().waitFor();

        if (err == 0) {
            FileUtils.forceDelete(errFile);
            FileUtils.forceDelete(outFile);
            return null;
        } else {
            String res = "Result " + err + " of " + String.join(" ", mp4box.command());
            log.warning("Failed while concatenating chunks: "+err);
            return res+"\n"+FileUtils.readFileToString(outFile, "UTF-8")+"\n"+FileUtils.readFileToString(errFile, "UTF-8")+"\nEOM\n";
        }
    }

}
