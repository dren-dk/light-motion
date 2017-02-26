package dk.dren.lightmotion.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Finds files in a directory, with given suffix and returns them sorted by last modified timestamp and then by name
 */
public class SortedDir {
    private final File dir;
    private final String fileNameSuffix;

    public SortedDir(File dir, String fileNameSuffix) {
        this.dir = dir;
        this.fileNameSuffix = fileNameSuffix;

        if (!dir.isDirectory() || !dir.canRead()) {
            throw new IllegalArgumentException("The directory " + dir + " is not a directory that can be read!");
        }
    }

    public List<File> list() throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            throw new IOException("The directory "+dir+" has disappeared, I will not stand for such insolence!");
        }

        List<File> fileList = new ArrayList<>();
        for (File f : files) {
            if (f.getName().endsWith(fileNameSuffix) && f.isFile()) {
                fileList.add(f);
            }
        }

        // Sort first by timestamp and then by name if that fails
        fileList.sort((f1,f2) -> {
            long m1 = f1.lastModified();
            long m2 = f2.lastModified();
            if (m1 != m2) {
                return Long.compare(m1, m2);
            } else {
                return f1.getName().compareTo(f2.getName());
            }
        });

        return fileList;
    }



}
