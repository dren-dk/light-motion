package dk.dren.lightmotion.image;

import dk.dren.lightmotion.core.snapshot.FixedPointPixels;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class LoadPPM {
    @Test
    public void loadPPM() throws IOException {
        long t0 = System.currentTimeMillis();
        final BufferedImage image = ImageIO.read(LoadPPM.class.getResourceAsStream("/frame0007.pnm"));
        Assert.assertNotNull(image);
        long t1 = System.currentTimeMillis();
        System.out.println("Loaded image in "+(t1-t0)+" ms");

        new FixedPointPixels("test", image);
        long t2 = System.currentTimeMillis();
        System.out.println("Converted image in "+(t2-t1)+" ms  total: "+(t2-t0));

        byte[] bytes = IOUtils.toByteArray(LoadPPM.class.getResourceAsStream("/frame0007.pnm"));
        long t3 = System.currentTimeMillis();
        System.out.println("Loaded bytes from classpath in "+(t3-t2)+" ms");

        FixedPointPixels test = FixedPointPixels.readFromAnyBytes("test", bytes);
        long t4 = System.currentTimeMillis();
        System.out.println("Loaded ppm bytes in "+(t4-t3)+" ms");

        test.write(new File("/tmp/snapshot.png"));
        long t5 = System.currentTimeMillis();
        System.out.println("Wrote png in "+(t5-t4)+" ms");
    }

    @Test
    public void loadPPM1() throws IOException {
        for (int i=0;i<100;i++) {
            loadPPM();
        }
    }

    }
