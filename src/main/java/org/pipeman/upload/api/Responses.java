package org.pipeman.upload.api;

import io.javalin.http.Context;

import java.util.Map;

public enum Responses {
    INVALID_UPLOAD_ID(0, "Invalid upload ID; the upload might have expired"),
    BODY_TOO_LARGE(1, "Request body is too large"),
    PASSWORD_MISSING(2, "Query param 'password' is missing"),
    PASSWORD_WRONG(3, "Password is wrong"),
    FILE_TOO_LARGE(4, "The uploaded file exceeded the maximum allowed filesize");

    private final String name;
    private final String description;
    private final int id;
    private final int status;

    Responses(int id, String description) {
        this(id, 400, description);
    }

    Responses(int id, int status, String description) {
        this.id = id;
        this.name = name().toLowerCase().replace('_', '-');
        this.description = description;
        this.status = status;
    }

    public void apply(Context ctx) {
        ctx.status(status).json(Map.of(
                "error", Map.of(
                        "id", id,
                        "name", name,
                        "description", description
                )
        ));
    }
}
