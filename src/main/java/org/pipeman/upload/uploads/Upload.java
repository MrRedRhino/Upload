package org.pipeman.upload.uploads;

import org.pipeman.upload.Config;
import org.pipeman.upload.utils.Utils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class Upload {
    private final UUID id;
    private final OutputStream os;
    private final String fileId;

    private long lastWrite = System.currentTimeMillis();

    private final String filename;
    private final String password;
    private final long deleteDelay;
    private final int maxDownloads;


    public Upload(UUID id, OutputStream os, String fileId, String filename, String password, long deleteDelay, int maxDownloads) {
        this.id = id;
        this.os = os;
        this.fileId = fileId;
        this.filename = filename;
        this.password = password;
        this.deleteDelay = deleteDelay;
        this.maxDownloads = maxDownloads;
    }

    public static Upload create(String password, String filename, long deleteDelay, int maxDownloads) throws IOException {
        String fileId = Utils.genRandomString(Config.PROVIDER.c().fileIdLength);
        Path p = Utils.createUploadPath(fileId);
        p.getParent().toFile().mkdirs();

        return new Upload(UUID.randomUUID(), Files.newOutputStream(Utils.createUploadPath(fileId)), fileId,
                filename, password, deleteDelay, maxDownloads);
    }

    public UUID id() {
        return id;
    }

    public void write(byte[] data) throws IOException {
        os.write(data);
        lastWrite = System.currentTimeMillis();
    }

    public void close() throws IOException {
        os.close();
    }

    public void deleteFile() throws IOException {
        os.close();
        Utils.createUploadPath(fileId).toFile().delete();
    }

    public String getFileId() {
        return fileId;
    }

    public long lastWrite() {
        return lastWrite;
    }

    public UploadMeta createMeta() {
        return new UploadMeta(password, filename, Utils.timeSeconds() + deleteDelay, maxDownloads, 0);
    }
}
