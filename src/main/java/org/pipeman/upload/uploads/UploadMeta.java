package org.pipeman.upload.uploads;

import org.pipeman.upload.utils.ByteUtils;

import java.util.Arrays;

public record UploadMeta(String password, String filename, long deleteTimestamp, int maxDownloads, int downloads) {
    private static final int TOTAL_SIZE = 144;

    public byte[] serialize() {
        byte[] out = new byte[TOTAL_SIZE];
        System.arraycopy(ByteUtils.longToBytes(deleteTimestamp), 0, out, 0, 8);
        System.arraycopy(ByteUtils.intToBytes(downloads), 0, out, 8, 4);
        System.arraycopy(ByteUtils.intToBytes(maxDownloads), 0, out, 12, 4);

        System.arraycopy(ByteUtils.stringToBytes(filename), 0, out, 16, 64);
        System.arraycopy(ByteUtils.stringToBytes(password), 0, out, 80, 64);
        return out;
    }

    public static UploadMeta deserialize(byte[] source) {
        long ts = ByteUtils.bytesToLong(Arrays.copyOf(source, 8));
        int downloads = ByteUtils.bytesToInt(Arrays.copyOfRange(source, 8, 12));
        int maxDownloads = ByteUtils.bytesToInt(Arrays.copyOfRange(source, 12, 16));

        String filename = ByteUtils.bytesToString(Arrays.copyOfRange(source, 16, 80));
        String password = ByteUtils.bytesToString(Arrays.copyOfRange(source, 80, TOTAL_SIZE));
        return new UploadMeta(password, filename, ts, maxDownloads, downloads);
    }

    public UploadMeta increaseDownloads(int amount) {
        return new UploadMeta(password, filename, deleteTimestamp, maxDownloads, downloads + amount);
    }
}
