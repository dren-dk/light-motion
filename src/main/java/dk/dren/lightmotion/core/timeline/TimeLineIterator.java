package dk.dren.lightmotion.core.timeline;

import dk.dren.lightmotion.core.events.LightMotionEvent;

import java.util.Iterator;

/**
 * An Iterator that iterates over events in the TimeLine, across SubTimeLines
 */
public class TimeLineIterator implements Iterator<LightMotionEvent> {
    private final TimeLine timeLine;
    private SubTimeLine subTimeLine;
    private LightMotionEvent next;

    public TimeLineIterator(TimeLine timeLine, long timestamp) {
        this.timeLine = timeLine;
        subTimeLine = timeLine.findSubTimeLineContaining(timestamp);
        if (subTimeLine == null) {
            subTimeLine = timeLine.firstTimeLine();
        }

        next(timestamp);
    }

    private void next(long timestamp) {
        if (subTimeLine != null) {
            next = subTimeLine.nextEvent(timestamp);
            if (next == null) {
                subTimeLine = timeLine.findSubTimeLineAfter(timestamp);
                if (subTimeLine != null) {
                    next = subTimeLine.firstEvent();
                } else {
                    next = null;
                }
            }
        }
    }

    @Override
    public boolean hasNext() {
        return next != null;
    }

    @Override
    public LightMotionEvent next() {
        LightMotionEvent result = next;
        next(result.getTimestamp());
        return result;
    }
}
