package dk.dren.lightmotion.core;

import dk.dren.lightmotion.core.events.LightMotionEvent;
import dk.dren.lightmotion.core.snapshot.SnapshotProcessingManager;
import dk.dren.lightmotion.onvif.ONVIFCamera;
import dk.dren.lightmotion.onvif.ONVIFProfile;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.xml.sax.SAXException;

import javax.xml.soap.SOAPException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Read a camera, this means two things:
 * 1: Run a thread that periodically polls the snapshot url to get a snapshot, which is then fed into the snapshot queue.
 * 2: Run a thread that starts the external streamer process and waits for it to quit and if it does, then restarts it.
 *
 * TODO: Split out the 3 threads that manage external processes, so this class gets smaller.
 */
@Log
@RequiredArgsConstructor
@Getter
public class CameraManager implements dk.dren.lightmotion.core.events.LightMotionEventSink {
    private final LightMotion lightMotion;
    private final CameraConfig cameraConfig;
    private Thread snapshotThread;
    private Thread streamThread;
    private ONVIFCamera onvif;
    private boolean keepRunning = true;
    private String error;
    private Integer framerate;
    private Integer width;
    private Integer height;
    private Process streamProcess;
    private boolean streamProcessRunning;
    private boolean streamProcessKilling;
    private SnapshotProcessingManager snapshotProcessingManager;
    private ONVIFProfile highresProfile;
    private ONVIFProfile lowresProfile;
    private Process lowresStreamProcess;
    private boolean lowresStreamProcessRunning;
    private boolean lowresStreamProcessKilling;
    private Thread lowresSnapshotThread;
    private MovieMaker movieMaker;

    void start() {

        snapshotThread = new Thread(() -> {
            try {
                LightMotion.mkdir(getChunkDir(), "camera-chunks");
                LightMotion.mkdir(getStateDir(), "camera-state");
                LightMotion.mkdir(getRecordingDir(), "camera-recording");
                LightMotion.mkdir(getWorkingDir(), "camera-working");

                movieMaker = new MovieMaker(this);
                snapshotProcessingManager = new SnapshotProcessingManager(this);
                interrogateOnvif();
                startStreamThread();
                if (cameraConfig.isLowresSnapshot()) {
                    lowresHttpJpegSnapshots();
                } else {
                    lowresStreamSnapshots();
                }
            } catch (Throwable e) {
                if (keepRunning) {
                    log.log(Level.SEVERE, "Failed in the snapshot thread for " + cameraConfig.getName() + ": ", e);
                    error = "Snapshot thread exited " + e.toString();
                }
            }
        });
        snapshotThread.setName("Polling snapshots from "+cameraConfig.getName());
        snapshotThread.setDaemon(true);
        snapshotThread.start();
    }

    void stop() {
        keepRunning = false;
        snapshotThread.interrupt();
        streamThread.interrupt();
    }

    private void interrogateOnvif() throws SOAPException, SAXException, IOException {
        onvif = new ONVIFCamera(cameraConfig.getAddress(), cameraConfig.getUser(), cameraConfig.getPassword());

        highresProfile = onvif.getProfiles().get(cameraConfig.getProfileNumber());
        lowresProfile = onvif.getProfiles().get(cameraConfig.getLowresProfileNumber());

        framerate = cameraConfig.getForceFramerate() != null
                ? cameraConfig.getForceFramerate()
                : highresProfile.getFramerate();

        if (framerate == null) {
            error = "Neither ONVIF nor YAML has a framerate for "+cameraConfig.getName()+" add forceFramerate=... to the camera section in the YAML file";
            log.severe(error);
            throw new RuntimeException(error);
        }

        width = cameraConfig.getForceWidth() != null
                ? cameraConfig.getForceWidth()
                : highresProfile.getWidth();
        if (width == null) {
            error = "Neither ONVIF nor YAML has a width for "+cameraConfig.getName()+" add forceWidth=... to the camera section in the YAML file";
            log.severe(error);
            throw new RuntimeException(error);
        }

        height = cameraConfig.getForceHeight() != null
                ? cameraConfig.getForceHeight()
                : highresProfile.getHeight();
        if (height == null) {
            error = "Neither ONVIF nor YAML has a height for "+cameraConfig.getName()+" add forceHeight=... to the camera section in the YAML file";
            log.severe(error);
            throw new RuntimeException(error);
        }
    }


    private void startStreamThread() {
        if (System.getProperty("nostream","").equals("true")) {
            return;
        }

        snapshotThread.setName("Polling snapshots from "+cameraConfig.getName()+" via "+lowresProfile.getSnapshotUri());
        streamThread = new Thread(() -> {
            try {
                streamer();
            } catch (Throwable e) {
                if (!keepRunning) {
                    log.log(Level.SEVERE, "Failed in the streaming thread for " + cameraConfig.getName() + ": ", e);
                    error = "Streaming thread exited " + e.toString();
                }
            }
        });
        streamThread.setName("Streaming from "+cameraConfig.getName()+" via "+lowresProfile.getSnapshotUri());
        streamThread.setDaemon(true);
        streamThread.start();
    }

    public static String getTimeStamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        return sdf.format(new Date());
    }

    public File getChunkDir() {
        return new File(lightMotion.getConfig().getChunkRoot(), cameraConfig.getName());
    }

    public File getWorkingDir() {
        return new File(lightMotion.getConfig().getWorkingRoot(), cameraConfig.getName());
    }

    public File getRecordingDir() {
        return new File(lightMotion.getConfig().getRecordingRoot(), cameraConfig.getName());
    }

    public File getStateDir() {
        return new File(lightMotion.getConfig().getStateRoot(), cameraConfig.getName());
    }

    private long getStreamerPid() {
        // TODO: Once Java 9 comes out we finally get Process.getPid() and we can get rid of this bullshit
        try {
            if (streamProcess.getClass().getName().equals("java.lang.UNIXProcess")) {
                Field f = streamProcess.getClass().getDeclaredField("pid");
                f.setAccessible(true);
                return ((Integer)f.get(streamProcess)).longValue();
            } else {
                throw new RuntimeException("Unsupported process class: "+streamProcess.getClass().getName());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final Object KILL_LOCK = new Object();
    private void killStreamer() throws IOException, InterruptedException {
        synchronized (KILL_LOCK) {
            streamProcessKilling = true;
            long streamerPid = getStreamerPid();
            killProcess(streamerPid, "HUB"); // Ask nicely
            if (streamProcessKilling) {
                if (!streamProcess.waitFor(5, TimeUnit.SECONDS)) {
                    killProcess(streamerPid, "KILL");
                }
            }
        }
    }

    private static void killProcess(long streamerPid, String signal) throws InterruptedException, IOException {
        ProcessBuilder killer = new ProcessBuilder("kill", "-"+signal, Long.toString(streamerPid));
        killer.inheritIO().start().waitFor();
    }

    private void streamer() throws IOException, InterruptedException {

        while (keepRunning) {
            String timestamp = getTimeStamp();

            ProcessBuilder pb = new ProcessBuilder("ffmpeg",
                    "-i", highresProfile.getStreamUrl(), "-map", "0",
                    "-probesize", "32",
                    "-f", "segment", "-segment_time", lightMotion.getConfig().getChunkLength().toString(), "-segment_atclocktime", "1", "-segment_format", "mp4",
                    "-vcodec", "copy", "-an",
                    timestamp+"-%010d.mp4");
            pb.directory(getChunkDir());

            log.info("Running: "+String.join(" ", pb.command()));
            streamProcess = pb.start();
            StreamFifoLogger.glom(streamProcess.getErrorStream(), new File(getChunkDir(), "ffmpeg.err"));
            StreamFifoLogger.glom(streamProcess.getInputStream(), new File(getChunkDir(), "ffmpeg.out"));
            streamProcessRunning = true;
            int err = -1;
            try {
                err = streamProcess.waitFor();
                log.info("Exit code from ffmpeg streamer was: "+err);
            } finally {
                streamProcessRunning = false;
                streamProcessKilling = false;
            }
            if (err != 0) {
                Thread.sleep(1000); // Don't burn all the CPU in case of an error.
            }
        }

        if (streamProcessRunning) {
            killStreamer();
        }
    }

    private void openRTSPstreamer() throws IOException, InterruptedException {

        while (keepRunning) {
            //  ffmpeg -i rtsp://10.0.2.97:554/11 -map 0 -f segment -segment_time 60 -segment_atclocktime 1 -segment_format mp4 -vcodec copy -an "capture-%010d.mp4"

            String timestamp = getTimeStamp();

            List<String> cmd = new ArrayList<>();
            cmd.add(lightMotion.getOpenRTSP().getAbsolutePath());
            cmd.add("-v"); // Stream only video
            cmd.add("-4"); // Store mp4
            cmd.add("-p"); cmd.add(Integer.toString(cameraConfig.getRtspClientPort())); // Large buffer size
            cmd.add("-b"); cmd.add("500000"); // Large buffer size
            cmd.add("-f"); cmd.add(framerate.toString()); // Set framerate
            cmd.add("-w"); cmd.add(width.toString()); // Set width
            cmd.add("-h"); cmd.add(height.toString()); // Set height
            cmd.add("-F"); cmd.add(getChunkDir().getAbsolutePath()+"/"+timestamp); // The output directory + file prefix
            cmd.add("-P"); cmd.add(lightMotion.getConfig().getChunkLength().toString()); // Length of the chunks to record
            cmd.add(highresProfile.getStreamUrl());

            log.info("Running: "+String.join(" ", cmd));
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectError(new File(getChunkDir(), "openrtsp.log"));
            pb.redirectOutput(new File("/dev/null"));
            pb.directory(getChunkDir());

            streamProcess = pb.start();
            streamProcessRunning = true;
            int err = -1;
            try {
                err = streamProcess.waitFor();
                log.info("Exit code from openRTSP was: "+err);
            } finally {
                streamProcessRunning = false;
                streamProcessKilling = false;
            }
            if (err != 0) {
                Thread.sleep(1000); // Don't burn all the CPU in case of an error.
            }
        }

        if (streamProcessRunning) {
            killStreamer();
        }
    }


    /**
     * fetches a snapshot from the camera and stores it in the queue for the main thread to take care of
     *
     * ffmpeg -probesize 32 -i rtsp://10.0.2.93:554/12 -r 1/1 -f image2 frame%04d.pnm
     */
    private void lowresHttpJpegSnapshots() throws InterruptedException, IOException {
        try (CloseableHttpClient hc = HttpClients.createDefault()) {
            while (keepRunning) {

                log.fine("Fetching "+lowresProfile.getSnapshotUri());
                HttpGet req = new HttpGet(lowresProfile.getSnapshotUri());
                try (CloseableHttpResponse response = hc.execute(req)) {
                    if (response.getStatusLine().getStatusCode() != 200) {
                        log.severe("Got error response "+response.getStatusLine().getStatusCode()+" from "+lowresProfile.getSnapshotUri());
                        Thread.sleep(5000);

                    } else {
                        byte[] imageBytes;
                        try (InputStream content = response.getEntity().getContent()) {
                            imageBytes = IOUtils.toByteArray(content);
                        }

                        String imageName = cameraConfig.getName()+"-"+getTimeStamp();

                        // This might block while waiting for room in the queue, so we do this after closing the http response
                        lightMotion.getSnapshots().add(new CameraSnapshotByteArray(snapshotProcessingManager, imageName, imageBytes));
                    }

                } catch (Exception e) {
                    log.warning("Failed while requesting "+lowresProfile.getSnapshotUri()+" "+e);
                }

                Thread.sleep(lightMotion.getConfig().getPollInterval());
            }
        }
    }

    private void lowresStreamSnapshots() throws InterruptedException, IOException {
        File lowresDir = getWorkingDir();

        lowresSnapshotThread = new Thread(() -> {
            try {
                lowresSnapshotLoader();
            } catch (Throwable e) {
                if (!keepRunning) {
                    log.log(Level.SEVERE, "Failed in the lowres loading thread for " + cameraConfig.getName() + ": ", e);
                    error = "Lowres loading thread exited " + e.toString();
                }
            }
        });
        lowresSnapshotThread.setName("Loading lowres snapshots from "+cameraConfig.getName()+" via "+lowresProfile.getSnapshotUri());
        lowresSnapshotThread.setDaemon(true);
        lowresSnapshotThread.start();

        while (keepRunning) {

            // ffmpeg -probesize 32 -i rtsp://10.0.2.93:554/12 -r 1/1 -f image2 frame%04d.pnm
            List<String> cmd = new ArrayList<>();
            cmd.add(lightMotion.getFfmpeg().getAbsolutePath());
            cmd.add("-probesize");
            cmd.add("32");
            cmd.add("-i");
            cmd.add(lowresProfile.getStreamUrl());
            cmd.add("-r");
            cmd.add("1/1");
            cmd.add("-f");
            cmd.add("image2");
            cmd.add("frame%04d.ppm");

            log.info("Running: "+String.join(" ", cmd));
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectError(new File(lowresDir, "ffmpeg.log"));
            pb.redirectOutput(new File("/dev/null"));
            pb.directory(lowresDir);

            lowresStreamProcess = pb.start();
            lowresStreamProcessRunning = true;
            int err = -1;
            try {
                err = lowresStreamProcess.waitFor();
                log.info("Exit code from ffmpeg was: "+err);
            } finally {
                lowresStreamProcessRunning = false;
                lowresStreamProcessKilling = false;
            }
            if (err != 0) {
                Thread.sleep(1000); // Don't burn all the CPU in case of an error.
            }
        }
    }

    private void lowresSnapshotLoader() throws InterruptedException, IOException {
        SortedDir sortedWorkingDir = new SortedDir(getWorkingDir(), ".ppm");

        while (keepRunning) {
            Thread.sleep(500);

            List<File> fileList = sortedWorkingDir.list();
            if (fileList.size() < 2) {
                continue; // Wait until there's at least two files in the buffer
            }
            fileList.remove(fileList.size()-1); // Don't load the newest file.

            for (File file : fileList) {
                String imageName = cameraConfig.getName()+"-"+getTimeStamp();

                CameraSnapshotFile sn = new CameraSnapshotFile(snapshotProcessingManager, imageName, file);
                if (lightMotion.getSnapshots().offer(sn)) {
                    sn.getImageBytes(); // Force loading the bytes into memory, so decoding can happen in the loading thread.
                }
                file.delete();
            }
        }
    }

    @Override
    public void notify(LightMotionEvent event) {
        lightMotion.notify(event);
        movieMaker.notify(event);
    }
}
