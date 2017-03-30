package dk.dren.lightmotion.core.events;

import dk.dren.lightmotion.core.LightMotion;
import dk.dren.lightmotion.core.timeline.TimeStamps;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.text.ParseException;
import java.util.IllegalFormatException;

/**
 * An event that has happend in the system
 */
@RequiredArgsConstructor
@Getter
public class LightMotionEvent {
    private final long timestamp;
    private final LightMotionEventType type;
    private final boolean canceling;
    private final String cameraName;
    private final String text;

    public static LightMotionEvent start(LightMotionEventType type, String cameraName, String text) {
        return new LightMotionEvent(System.currentTimeMillis(), type, false, cameraName, text);
    }

    public static LightMotionEvent end(LightMotionEventType type, String cameraName, String text) {
        return new LightMotionEvent(System.currentTimeMillis(), type, true, cameraName, text);
    }

    @Override
    public String toString() {
        return (canceling ? "!" : "") +  type + " from " + cameraName+": " + text;
    }

    public String store() {
        return TimeStamps.formatMs(timestamp)+"\t"  // 0
                +(canceling?"0":"1")+"\t"            // 1
                +type+"\t"                          // 2
                +cameraName+"\t"                    // 3
                +text;                              // 4
    }

    public static LightMotionEvent load(String string) throws ParseException {
        String[] strings = string.split("\t");
        if (strings.length != 5) {
            throw new ParseException("Cannot parse '"+string+"' it doesn't have 5 tab-separated parts, it has "+strings.length, 0);
        }
        return new LightMotionEvent(TimeStamps.parseMs(strings[0]),
                LightMotionEventType.valueOf(strings[2]),
                strings[1].equals("!"),
                strings[3],
                strings[4]);
    }
}
