package dk.dren.lightmotion.core.timeline;

import dk.dren.lightmotion.core.events.LightMotionEvent;
import dk.dren.lightmotion.core.events.LightMotionEventType;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

public class TimeLineTest {

    @Test
    public void testAppend() throws ParseException, IOException {
        long time = TimeStamps.parseMs("20170101_000000.000");
        File dir = File.createTempFile("timeline-test.", ".dir");
        FileUtils.forceDelete(dir);
        FileUtils.forceMkdir(dir);
        try {
            final int eventCount = 100*1000;

            long t0 = System.currentTimeMillis();
            TimeLine tl = new TimeLine(10000, dir);
            for (int i = 0; i < eventCount; i++) {
                time += 1+Math.random() * 10 * 60 * 1000;
                tl.append(new LightMotionEvent(time, LightMotionEventType.MOTION, (i & 1) != 0, "test", "Hello " + i));
            }

            long t1 = System.currentTimeMillis();
            int read = 0;
            for (LightMotionEvent lightMotionEvent : tl.eventsAfter(0)) {
                //System.out.println("Found: "+read+": "+lightMotionEvent.store());
                String text = "Hello "+read;
                if (!text.equals(lightMotionEvent.getText())) {
                    System.out.println("Mismatch: "+text+" "+lightMotionEvent.getText());
                }
               // Assert.assertEquals(text, lightMotionEvent.getText());
                read++;
            }
            long t2 = System.currentTimeMillis();

            System.out.println("Insert: "+(t1-t0)+" ms, read: "+(t2-t1)+" ms");

            Assert.assertEquals(eventCount, read);

        } finally {
            FileUtils.forceDelete(dir);
        }
    }
}