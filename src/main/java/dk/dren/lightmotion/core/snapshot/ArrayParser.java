package dk.dren.lightmotion.core.snapshot;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Parse through an array of bytes, one at a time, while collecting a String
 */
@RequiredArgsConstructor
@Getter
public class ArrayParser {
    final private byte[] bytes;
    private int pos = 0;

    public String readLine() {
        StringBuilder buffy = new StringBuilder();

        while (bytes[pos] != '\n' && pos < 100) {
            buffy.append((char)bytes[pos]);
            pos++;
        }
        pos++; // Skip the newline

        return buffy.toString();
    }
}
