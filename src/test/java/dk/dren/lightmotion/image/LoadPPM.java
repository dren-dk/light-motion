package dk.dren.lightmotion.image;

import org.junit.Assert;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class LoadPPM {
    @Test
    public void loadPPM() throws IOException {
        final BufferedImage image = ImageIO.read(LoadPPM.class.getResourceAsStream("/frame0007.pnm"));
        Assert.assertNotNull(image);
    }

}
