package dk.dren.lightmotion.core.snapshot;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class MotionDetectionResult {
    private final FixedPointPixels diffImage;
    private final int maxDiff;
    private final int maxDiffX;
    private final int maxDiffY;
}
