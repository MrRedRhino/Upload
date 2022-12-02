package org.pipeman.upload.uploads;

import org.pipeman.upload.Config;
import org.pipeman.upload.utils.Database;
import org.pipeman.upload.utils.Utils;

import javax.naming.SizeLimitExceededException;
import java.io.IOException;
import java.util.*;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class UploadManager {
    public static final UploadManager INSTANCE = new UploadManager();
    private final Map<UUID, Upload> uploads = new HashMap<>();

    public UploadManager() {
        new Timer().schedule(new UploadCleaner(this), 0, 10_000);
    }

    public Upload createUpload(String password, String filename, long deleteDelay, int maxDownloads) throws IOException {
        Upload newUpload = Upload.create(password, filename, deleteDelay, maxDownloads);
        uploads.put(newUpload.id(), newUpload);
        return newUpload;
    }

    public boolean writeToUpload(UUID uploadId, byte[] data) throws IOException, SizeLimitExceededException {
        Upload upload = uploads.get(uploadId);
        if (upload == null) return false;
        int maxSize = Config.PROVIDER.c().maximumFileSize;
        if (maxSize != -1 && upload.size() + data.length < maxSize) upload.write(data);
        else throw new SizeLimitExceededException();
        return true;
    }

    public String finishUpload(UUID uploadId) throws IOException {
        Upload upload = uploads.get(uploadId);
        if (upload == null) return "";
        upload.close();

        Database.INSTANCE.getDB().put(upload.getFileId().getBytes(), upload.createMeta().serialize());
        uploads.remove(uploadId);
        return upload.getFileId();
    }

    public void cancelUpload(UUID uploadId) throws IOException {
        Upload upload = uploads.get(uploadId);
        if (upload == null) return;
        upload.deleteFile();
        uploads.remove(uploadId);
    }

    public UploadMeta increaseDownloadCount(UploadMeta meta, String fileId) {
        UploadMeta newMeta = meta.increaseDownloads(1);
        Database.INSTANCE.getDB().put(fileId.getBytes(), newMeta.serialize());
        return newMeta;
    }

    public boolean hasExpired(UploadMeta meta) {
        return meta.deleteTimestamp() != -1 && meta.deleteTimestamp() < Utils.timeSeconds();
    }

    public void deleteFile(String fileId) {
        Utils.createUploadPath(fileId).toFile().delete();
    }

    public void deleteDBEntry(String fileId) {
        Database.INSTANCE.getDB().delete(fileId.getBytes());
    }

    public static class UploadCleaner extends TimerTask {
        private final UploadManager target;

        public UploadCleaner(UploadManager uploadManager) {
            target = uploadManager;
        }

        @Override
        public void run() {
            List<Map.Entry<UUID, Upload>> toRemove = new ArrayList<>();

            for (Map.Entry<UUID, Upload> e : target.uploads.entrySet()) {
                if (e.getValue().lastWrite() < System.currentTimeMillis() - Config.PROVIDER.c().maximumIdle) {
                    try {
                        target.cancelUpload(e.getKey());
                    } catch (IOException ignored) {
                    }
                    toRemove.add(e);
                }
            }
            toRemove.forEach(target.uploads.entrySet()::remove);
        }
    }
}
