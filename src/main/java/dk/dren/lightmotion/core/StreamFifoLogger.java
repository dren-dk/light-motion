package dk.dren.lightmotion.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.io.*;
import java.util.logging.Level;

/**
 * This class provides in-memory log-rotation for busy streams that don't really contain all that much interesting output
 *
 * Reads from a stream until there's an error.
 * Every once in a while the last n lines are written to the file.
 * Excessive lines are quietly dropped.
 */
@RequiredArgsConstructor
@Log
public class StreamFifoLogger {
    private static final int DEFAULT_INTERVAL = 30000;
    private static final int DEFAULT_HISTORY = 150;
    private final InputStream stream;
    private final File file;
    private final int interval;
    private final int history;

    private Thread thread;
    private CircularFifoQueue<String> fifo;
    private long nextWrite;

    public static StreamFifoLogger glom(InputStream stream, File file) {
        return glom(stream, file, DEFAULT_INTERVAL, DEFAULT_HISTORY);
    }

    public static StreamFifoLogger glom(InputStream stream, File file, int interval, int history) {
        StreamFifoLogger instance = new StreamFifoLogger(stream, file, interval, history);
        instance.start();
        return instance;
    }

    private void start() {
        fifo = new CircularFifoQueue<>(history);
        thread = new Thread(() -> {
            try {
                readForever();
            } catch (Exception e) {
                log.log(Level.INFO, "Caught exception, perhaps it's nothing", e);
            }
            try {
                writeFile();
            } catch (Exception e) {
                log.log(Level.WARNING, "Caught exception, while writing the fifo to "+file, e);
            }
        });

        thread.setName("Writing "+file);
        thread.setDaemon(true);
        thread.start();
    }

    private void writeFile() throws FileNotFoundException {
        synchronized (file) {
            try {
                if (fifo.isEmpty() && file.exists()) {
                    file.delete(); // We don't care about empty files, at all.
                } else {
                    try (PrintStream ps = new PrintStream(new FileOutputStream(file))) {
                        for (String line : fifo) {
                            ps.println(line);
                        }
                    }
                }
            } finally {
                nextWrite = System.currentTimeMillis() + interval;
            }
        }
    }

    private void readForever() throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = br.readLine()) != null) {
                synchronized (file) {
                    fifo.add(line);
                }

                if (System.currentTimeMillis() > nextWrite) {
                    writeFile();
                }
            }
        }
    }
}
