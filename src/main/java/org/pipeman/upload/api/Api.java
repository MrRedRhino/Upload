package org.pipeman.upload.api;

import io.javalin.http.Context;
import io.javalin.http.InternalServerErrorResponse;
import org.pipeman.upload.Config;
import org.pipeman.upload.uploads.Upload;
import org.pipeman.upload.uploads.UploadManager;
import org.pipeman.upload.uploads.UploadMeta;
import org.pipeman.upload.utils.Database;
import org.pipeman.upload.utils.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class Api {
    public static void createUpload(Context ctx) {
        try {
            String password = Utils.getOrElse(ctx.header("password"), "");
            String filename = Utils.getOrElse(ctx.header("filename"), "");
            long delDelay = Math.max(Utils.getLong(ctx.header("delete-delay")).orElse(604_800L), 300); // default 7 days, minimum 5 minutes
            int maxDownloads = Utils.getInt(ctx.header("max-download-count")).orElse(-1);
            if (maxDownloads == 0) maxDownloads = -1;

            Upload upload = UploadManager.INSTANCE.createUpload(password, filename, delDelay, maxDownloads);
            ctx.json(Map.of("upload-id", upload.id().toString()));
        } catch (IOException ignored) {
            throw new InternalServerErrorResponse();
        }
    }

    public static void writeToUpload(Context ctx) {
        Optional<UUID> id = getUUID(ctx);
        if (id.isEmpty()) {
            ctx.json(Map.of("errors", "invalid-upload-id"));
            ctx.status(400);
        } else {
            byte[] data = ctx.bodyAsBytes();

            int maxSize = Config.PROVIDER.c().maximumChunkSize;
            if (data.length == 0 || (maxSize != -1 && data.length > maxSize)) {
                ctx.json(Map.of("error", "invalid-body-length"));
                ctx.status(400);
                return;
            }

            try {
                if (!UploadManager.INSTANCE.writeToUpload(id.get(), data)) {
                    ctx.json(Map.of("error", "invalid-upload-id"));
                    ctx.status(400);
                }
            } catch (IOException ignored) {
                throw new InternalServerErrorResponse();
            }
        }
    }

    public static void closeUpload(Context ctx) {
        Optional<UUID> id = getUUID(ctx);
        if (id.isEmpty()) {
            ctx.json(Map.of("error", "invalid-upload-id"));
            ctx.status(400);
        } else {
            try {
                String fileId = UploadManager.INSTANCE.finishUpload(id.get());
                if (fileId.isEmpty()) {
                    ctx.json(Map.of("error", "invalid-upload-id"));
                    ctx.status(400);
                } else ctx.json(Map.of("file-id", fileId));
            } catch (IOException e) {
                throw new InternalServerErrorResponse();
            }
        }
    }

    private static Optional<UUID> getUUID(Context ctx) {
        String id = ctx.queryParam("id");
        if (id == null) return Optional.empty();
        try {
            return Optional.of(UUID.fromString(id));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public static void download(Context ctx) throws IOException {
        String s = Utils.getOrElse(ctx.pathParamMap().get("file-id"), "");

        byte[] data = getData(s);
        if (data == null) {
            ctx.html(Files.readString(Path.of("static", "upload-not-found.html")));
            return;
        }

        UploadMeta meta = UploadMeta.deserialize(data);
        if (meta.password().isEmpty()) {
            ctx.redirect("/api/download/" + s);
        } else {
            ctx.html(Files.readString(Path.of("static", "require-password.html")));
        }
    }

    public static void doDownload(Context ctx) {
        String s = ctx.pathParamMap().get("file-id");
        byte[] data = getData(s);
        if (data == null) {
            ctx.status(404);
            return;
        }

        UploadMeta meta = UploadMeta.deserialize(data);

        if (!meta.password().isEmpty()) {
            String password = ctx.queryParam("password");
            if (password == null) {
                ctx.json(Map.of("error", "password-missing"));
                ctx.status(400);
                return;
            }
            if (!password.equals(meta.password())) {
                ctx.json(Map.of("error", "invalid-password"));
                ctx.status(400);
                return;
            }
        }

        meta = UploadManager.INSTANCE.increaseDownloadCount(meta, s);

        if (UploadManager.INSTANCE.hasExpired(meta)) {
            ctx.status(404);
            UploadManager.INSTANCE.deleteDBEntry(s);
            UploadManager.INSTANCE.deleteFile(s);
            return;
        }

        boolean deleteFile = false;
        if (meta.maxDownloads() != -1 && meta.downloads() >= meta.maxDownloads()) {
            UploadManager.INSTANCE.deleteDBEntry(s);
            deleteFile = true;
        }

        Path path = Utils.createUploadPath(s);
        ctx.header("Content-Disposition", "attachment; filename=\"" + meta.filename() + "\"");
        ctx.header("Content-Length", path.toFile().length() + "");
        try {
            Files.copy(path, ctx.outputStream());
            if (deleteFile) UploadManager.INSTANCE.deleteFile(s);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static byte[] getData(String param) {
        if (param == null || param.isBlank()) return null;
        return Database.INSTANCE.getDB().get(param.getBytes());
    }
}
