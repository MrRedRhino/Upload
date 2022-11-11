package org.pipeman.upload;

import io.javalin.Javalin;
import org.pipeman.upload.api.Api;

import java.nio.file.Files;
import java.nio.file.Path;

import static io.javalin.apibuilder.ApiBuilder.*;

public class Main {
    public static void main(String[] args) {
        Javalin app = Javalin.create(c -> c.showJavalinBanner = false).start(Config.PROVIDER.c().serverPort);
        app.attribute("javalin-max-request-size", Config.PROVIDER.c().maximumChunkSize);

        app.routes(() -> {
            get("", ctx -> ctx.html(Files.readString(Path.of("static", "index.html"))));

            path("api", () -> {
                path("uploads", () -> {
                    post("create", Api::createUpload);
                    post("write", Api::writeToUpload);
                    post("close", Api::closeUpload);
                });
                get("download/{file-id}", Api::doDownload);

            });
            get("download/{file-id}", Api::download);
        });

        // TODO instant-upload for small files maybe?
        // DONE store filenames
        // DONE make files downloadable
        // DONE optional password
        // DONE delete after-option
        // DONE delete after download count option
    }
}