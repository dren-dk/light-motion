package dk.dren.lightmotion.core;

import dk.dren.lightmotion.core.events.LightMotionEventSink;
import dk.dren.lightmotion.db.Database;
import dk.dren.lightmotion.db.entity.Camera;
import dk.dren.lightmotion.db.entity.Event;
import io.dropwizard.lifecycle.Managed;
import lombok.Getter;
import lombok.extern.java.Log;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;

/**
 * The core class of the light motion system
 */
@Log
public class LightMotion implements Managed, LightMotionEventSink {
    @Getter
    private final Database database;
    @Getter
    private LightMotionConfig config;
    private Thread motionThread;
    private boolean keepRunning = true;
    private final Map<Long, CameraManager> cameraManagers = new TreeMap<>();
    @Getter
    private final ArrayBlockingQueue<CameraSnapshot> snapshots;

    public LightMotion(Database database, LightMotionConfig config) throws IOException {
        this.database = database;
        this.config = config;

        mkdir(config.getWorkingRoot(), "workingRoot");
        mkdir(config.getStateRoot(), "stateRoot");
        mkdir(config.getChunkRoot(), "chunkRoot");

        for (CameraConfig cameraConfig : config.getCameras()) {
            Camera oldCam = database.getCameraByName(cameraConfig.getName());

            Camera newCam = new Camera(null, null, cameraConfig.getName(), cameraConfig.getAddress(),
                    cameraConfig.getUser(), cameraConfig.getPassword(), cameraConfig.getProfileNumber(),
                    cameraConfig.getLowresProfileNumber(), cameraConfig.isLowresSnapshot());

            if (oldCam == null) {
                database.insertCamera(newCam);
            } else if (!oldCam.equals(newCam)) {
                database.updateCameraByName(newCam);
            }
        }

        for (Camera camera : database.getAllCameras()) {
            cameraManagers.put(camera.getId(), new CameraManager(this, camera));
        }

        snapshots = new ArrayBlockingQueue<>(cameraManagers.size()*2);
    }

    public static void mkdir(File dir, String name) {
        try {
            FileUtils.forceMkdir(dir);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to create the directory specified by "+name+"="+dir, e);
        }
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
            log.info("Starting camera manager "+cameraManager.getCamera().getName()+" with ONVIF address "+cameraManager.getCamera().getAddress());
            cameraManager.start();
            Thread.sleep(spread);
        }

        motionState("Started");
    }

    @Override
    public void stop() throws Exception {

    }

    @Override
    public void notify(Event event) {
        log.info("Event happened "+event);
    }

    public File getFfmpeg() {
        for (String path : System.getenv("PATH").split(File.pathSeparator)) {
            File f = new File(path+"/ffmpeg");
            if (f.isFile() && f.canExecute()) {
                return f;
            }
        }

        return null;
    }
}
