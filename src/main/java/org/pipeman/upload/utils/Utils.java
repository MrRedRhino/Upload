package org.pipeman.upload.utils;

import org.pipeman.upload.Config;

import java.nio.file.Path;
import java.security.SecureRandom;

public class Utils {
    private static final char[] ABC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final SecureRandom rnd = new SecureRandom();

    public static String genRandomString(int length){
        StringBuilder sb = new StringBuilder(length);
        for(int i = 0; i < length; i++) sb.append(ABC[rnd.nextInt(ABC.length)]);
        return sb.toString();
    }

    public static <T> T getOrElse(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }

    public static Path createUploadPath(String fileId) {
        return Path.of(Config.PROVIDER.c().uploadsDirectory, fileId);
    }

    public static long timeSeconds() {
        return System.currentTimeMillis() / 1000;
    }
}
