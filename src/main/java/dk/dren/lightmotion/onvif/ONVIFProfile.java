package dk.dren.lightmotion.onvif;

import lombok.Getter;
import lombok.Setter;
import org.w3c.dom.Element;

/**
 * The interesting bits of a profile
 */
@Getter
public class ONVIFProfile {
    private final String token;
    private final Integer framerate;
    private final Integer width;
    private final Integer height;
    @Setter
    private String streamUrl;
    @Setter
    private String snapshotUri;

    ONVIFProfile(Element e) {
        token = e.getAttribute("token");

        framerate = ONVIFCamera.xmlInt(e, "VideoEncoderConfiguration", "RateControl", "FrameRateLimit");
        width     = ONVIFCamera.xmlInt(e, "VideoEncoderConfiguration", "Resolution", "Width");
        height    = ONVIFCamera.xmlInt(e, "VideoEncoderConfiguration", "Resolution", "Height");
    }

    @Override
    public String toString() {
        return "ONVIFProfile{" +
                "token='" + token + '\'' +
                ", framerate=" + framerate +
                ", width=" + width +
                ", height=" + height +
                '}';
    }
}
