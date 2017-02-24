package dk.dren.lightmotion.core.snapshot;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Efficient parser for P6 images
 */
public class PPMParser {
    private static final Pattern SIZE = Pattern.compile("(\\d+) (\\d+)");

    public static FixedPointPixels readPPM6(String name, byte[] imageBytes) {
        ArrayParser ap = new ArrayParser(imageBytes);
        String type = ap.readLine();
        if (!type.equals("P6")) {
            throw new IllegalArgumentException("Cannot read image of type "+type);
        }

        String sizeLine = ap.readLine();
        Matcher size = SIZE.matcher(sizeLine);
        if (!size.matches()) {
            throw new IllegalArgumentException("Cannot parse size line: "+sizeLine);
        }

        String depth = ap.readLine();
        if (!depth.equals("255")) {
            throw new IllegalArgumentException("Bad color depth: "+depth);
        }

        int width = Integer.parseInt(size.group(1));
        int height = Integer.parseInt(size.group(2));
        int expectedImageBytes = width*height*3;

        int actualImageBytes = imageBytes.length - ap.getPos();
        if (actualImageBytes != expectedImageBytes) {
            throw new IllegalArgumentException("Bad number of bytes left for pixels, header said "+expectedImageBytes+" bytes, but there are "+actualImageBytes+" bytes");
        }

        FixedPointPixels result = new FixedPointPixels(name, width, height, false);
        int[] pixels = result.getPixels();
        if (pixels.length != expectedImageBytes) {
            throw new IllegalArgumentException("Bad number of long pixels there should be "+expectedImageBytes+" ints, but there are "+ pixels.length+" ints");
        }

        int input = ap.getPos();
        int output = 0;
        while (output < pixels.length) {
            byte r = imageBytes[input++];
            byte g = imageBytes[input++];
            byte b = imageBytes[input++];

            pixels[output++] = Byte.toUnsignedInt(g) << 16;
            pixels[output++] = Byte.toUnsignedInt(b) << 16;
            pixels[output++] = Byte.toUnsignedInt(r) << 16;
        }

        return result;
    }
}
