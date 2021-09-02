package utils;

import extension.RoomDuplicatorLauncher;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;

public class Cacher {
    public static final String dir;

    static {
        String tryDir;
        try {
            tryDir = new File(RoomDuplicatorLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParent() + "/cache";
        } catch (URISyntaxException e) {
            e.printStackTrace();
            tryDir = null;
        }
        dir = tryDir;
    }

    public static void updateCache(String content, String filename) {
        File parent_dir = new File(dir);
        parent_dir.mkdirs();

        try (FileWriter file = new FileWriter(new File(dir, filename))) {

            file.write(content);
            file.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
