package dk.dren.lightmotion.core.timeline;

import dk.dren.lightmotion.core.events.LightMotionEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.*;
import java.text.ParseException;
import java.util.Map;
import java.util.TreeMap;

/**
 * A chunk of the timeline, this is exactly the contents of a single file.
 */
@RequiredArgsConstructor
public class SubTimeLine {
    private final File file;
    private PrintStream printStream;
    private final TreeMap<Long, LightMotionEvent> events = new TreeMap<>();

    public static SubTimeLine load(File file) throws IOException, ParseException {
        SubTimeLine result = new SubTimeLine(file);

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line=br.readLine()) != null) {
                LightMotionEvent event = LightMotionEvent.load(line);
                result.events.put(event.getTimestamp(), event);
            }
        }

        return result;
    }

    public static SubTimeLine create(LightMotionEvent event, File file) throws FileNotFoundException {
        SubTimeLine result = new SubTimeLine(file);
        result.openWriter();
        result.append(event);
        return result;
    }

    void openWriter() throws FileNotFoundException {
        printStream = new PrintStream(new FileOutputStream(file, true));
    }

    public void append(LightMotionEvent event) {
        if (printStream == null) {
            throw new IllegalStateException("Not open for writing");
        }
        printStream.println(event.store());
        events.put(event.getTimestamp(), event);
    }

    public void closeWriter() {
        if (printStream != null) {
            printStream.close();
            printStream = null;
        }
    }

    public int getEventCount() {
        return events.size();
    }

    public LightMotionEvent nextEvent(LightMotionEvent event) {
        return nextEvent(event.getTimestamp());
    }

    public LightMotionEvent nextEvent(long timestamp) {
        Map.Entry<Long, LightMotionEvent> e = events.higherEntry(timestamp);
        if (e != null) {
            return e.getValue();
        } else {
            return null;
        }
    }

    public LightMotionEvent prevEvent(LightMotionEvent event) {
        Map.Entry<Long, LightMotionEvent> e = events.lowerEntry(event.getTimestamp()-1);
        if (e != null) {
            return e.getValue();
        } else {
            return null;
        }
    }

    public LightMotionEvent firstEvent() {
        Map.Entry<Long, LightMotionEvent> e = events.firstEntry();
        return e != null ? e.getValue() : null;
    }


    public LightMotionEvent lastEvent() {
        Map.Entry<Long, LightMotionEvent> e = events.lastEntry();
        return e != null ? e.getValue() : null;
    }
}
