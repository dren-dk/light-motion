package dk.dren.lightmotion.core.timeline;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeStamps {

    public static SimpleDateFormat secondsFormat() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss");
    }

    public static SimpleDateFormat msFormat() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss.SSS");
    }

    public static String getTimeStamp() {
        return formatMs(System.currentTimeMillis());
    }

    public static String formatMs(long time) {
        return msFormat().format(new Date(time));
    }

    public static long parseMs(String timestamp) throws ParseException {
        return msFormat().parse(timestamp).getTime();
    }

}
