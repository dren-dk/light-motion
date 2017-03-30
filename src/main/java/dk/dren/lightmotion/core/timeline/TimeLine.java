package dk.dren.lightmotion.core.timeline;

import dk.dren.lightmotion.core.events.LightMotionEvent;
import dk.dren.lightmotion.core.events.LightMotionEventSink;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Spliterator;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * A light-weight database of events indexed by the time they happened.
 * <p>
 * Insertions happen only at the end of the timeline.
 * The on-disk files are written to simply by appending new lines.
 * Deletions don't happen, but if they do, they happen by deleting the oldest files.
 * Each file has a maximum number of events in it, thus keeping the amount of data that needs to be in memory constant
 * and the number of files down to a minimum.
 */
@RequiredArgsConstructor
@Log
public class TimeLine implements LightMotionEventSink {
    public static final String DOT_TL = ".tl";

    private final int eventsPerSubTimeLine;
    private final File dir;

    private TreeSet<Long> knownSubTimeLines;
    private LinkedHashMap<Long, SubTimeLine> lru = new LinkedHashMap<Long, SubTimeLine>(4, 0.75f, true);
    private SubTimeLine appending;

    @Override
    public void notify(LightMotionEvent event) {
        try {
            append(event);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void append(LightMotionEvent event) throws IOException, ParseException {
        if (appending == null) {
            TreeSet<Long> knownSubTimeLines = getKnownSubTimeLines();

            if (knownSubTimeLines.isEmpty()) {
                appending = createSubTimeLine(event);
                return;
            } else {
                appending = getSubTimeLine(knownSubTimeLines.last());
                appending.openWriter();
            }
        }

        if (appending.getEventCount() >= eventsPerSubTimeLine) {
            appending.closeWriter();
            appending = createSubTimeLine(event);
            return;
        }

        LightMotionEvent last = appending.lastEvent();
        if (last.getTimestamp() > event.getTimestamp()) {
            throw new IllegalArgumentException("The new event to add is older than the latest event in the sub-timeline "+last+" > "+event);
        }
        appending.append(event);
    }

    private SubTimeLine createSubTimeLine(LightMotionEvent event) throws FileNotFoundException {
        SubTimeLine subTimeLine = SubTimeLine.create(event, getSubTimeLineFile(event.getTimestamp()));
        knownSubTimeLines.add(event.getTimestamp());
        return subTimeLine;
    }

    private SubTimeLine getSubTimeLine(Long startTime) throws IOException, ParseException {
        if (startTime == null) {
            return null;
        }
        SubTimeLine result = lru.get(startTime);
        if (result == null) {
            result = SubTimeLine.load(getSubTimeLineFile(startTime));

            lru.put(startTime, result);
        }
        return result;
    }

    private File getSubTimeLineFile(Long startTime) {
        return new File(dir, TimeStamps.formatMs(startTime) + DOT_TL);
    }

    private TreeSet<Long> getKnownSubTimeLines() {
        if (knownSubTimeLines == null) {
            knownSubTimeLines = new TreeSet<>();
            for (File e : dir.listFiles()) {
                String fn = e.getName();
                if (fn.endsWith(DOT_TL)) {
                    try {
                        long startTime = TimeStamps.parseMs(fn.substring(0, fn.length() - DOT_TL.length()));
                        knownSubTimeLines.add(startTime);
                    } catch (ParseException e1) {
                        log.log(Level.WARNING, "Ignoring file: " + e + " because the name isn't a parseable timestamp: ", e1);
                    }
                }
            }
        }

        return knownSubTimeLines;
    }

    public Iterable<LightMotionEvent> eventsAfter(final long timestamp) {
        return new Iterable<LightMotionEvent>() {

            @Override
            public Iterator<LightMotionEvent> iterator() {
                return new TimeLineIterator(TimeLine.this, timestamp);
            }

            @Override
            public void forEach(Consumer<? super LightMotionEvent> action) {
                Iterator<LightMotionEvent> iterator = iterator();
                while (iterator.hasNext()) {
                    action.accept(iterator.next());
                }
            }

            @Override
            public Spliterator<LightMotionEvent> spliterator() {
                return null;
            }
        };
    }

    SubTimeLine findSubTimeLineAfter(long timestamp) {
        Long st = getKnownSubTimeLines().higher(timestamp);
        try {
            return st != null ? getSubTimeLine(st) : null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    SubTimeLine findSubTimeLineContaining(long timestamp) {
        Long st = getKnownSubTimeLines().floor(timestamp);
        try {
            return st != null ? getSubTimeLine(st) : null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public SubTimeLine firstTimeLine() {
        Long st = getKnownSubTimeLines().first();
        try {
            return st != null ? getSubTimeLine(st) : null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
