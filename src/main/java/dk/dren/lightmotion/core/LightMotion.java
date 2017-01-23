package dk.dren.lightmotion.core;

import dk.dren.lightmotion.core.events.LightMotionEvent;
import dk.dren.lightmotion.core.events.LightMotionEventConsumer;
import io.dropwizard.lifecycle.Managed;
import lombok.Getter;
import lombok.extern.java.Log;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;

/**
 * The core class of the light motion system
 */
@Log
public class LightMotion implements Managed, LightMotionEventConsumer {
    @Getter
    private LightMotionConfig config;
    private Thread motionThread;
    private boolean keepRunning = true;
    private final Map<String, CameraManager> cameraManagers = new TreeMap<>();
    @Getter
    private final ArrayBlockingQueue<CameraSnapshot> snapshots;

    public LightMotion(LightMotionConfig config) throws IOException {
        this.config = config;

        FileUtils.forceMkdir(config.getRecordingRoot());
        FileUtils.forceMkdir(config.getWorkingRoot());

        extractOpenRTSP();

        for (CameraConfig cameraConfig : config.getCameras()) {
            cameraManagers.put(cameraConfig.getAddress(), new CameraManager(this, cameraConfig));
        }

        snapshots = new ArrayBlockingQueue<>(cameraManagers.size());
    }

    private void extractOpenRTSP() throws IOException {
        File fn = getOpenRTSP();
        try (InputStream is = LightMotion.class.getResourceAsStream("/openRTSP");
             OutputStream os = new FileOutputStream(fn)) {
            IOUtils.copy(is, os);
        }
        fn.setExecutable(true);
    }

    File getOpenRTSP() {
        return new File(config.getWorkingRoot(), "openRTSP");
    }

    private void motionState(String s) {
        motionThread.setName("Motion Detection ("+s+")");
    }

    @Override
    public void start() throws Exception {
        if (cameraManagers.isEmpty()) {
            log.severe("No cameras configured, nothing started");
            return;
        }
        motionThread = new Thread(() -> {
            try {
                startCameraThreads();
                motionState("Watching");
                while (keepRunning) {
                    checkForMotion();
                }
            } catch (Throwable t) {
                log.log(Level.SEVERE, "Failed in the motion detection thread, giving up and going home", t);
            }
        });
        motionThread.setDaemon(true);
        motionState("Starting");
        motionThread.start();
    }

    private void checkForMotion() throws InterruptedException {
        try {
            snapshots.take().processSnapshot();
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed while processing snapshot, ignoring", e);
        }
    }

    private void startCameraThreads() throws InterruptedException {
        int spread = config.getPollInterval()/cameraManagers.size();

        for (CameraManager cameraManager : cameraManagers.values()) {
            log.info("Starting camera manager "+cameraManager.getCameraConfig().getName()+" with ONVIF address "+cameraManager.getCameraConfig().getAddress());
            cameraManager.start();
            Thread.sleep(spread);
        }

        motionState("Started");
    }

    @Override
    public void stop() throws Exception {

    }

    @Override
    public void consumeEvent(LightMotionEvent event) {
        log.info("Event happened "+event);
    }
}
