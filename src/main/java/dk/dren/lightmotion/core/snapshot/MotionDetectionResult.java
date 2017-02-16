package dk.dren.lightmotion.core.snapshot;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class MotionDetectionResult {
    private final boolean movementDetected;
    private final int maxDiff;
    private final int maxDiffX;
    private final int maxDiffY;
    private final int threshold;
}
