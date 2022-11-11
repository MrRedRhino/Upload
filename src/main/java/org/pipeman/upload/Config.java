package org.pipeman.upload;

import org.pipeman.pconf.AbstractConfig;
import org.pipeman.pconf.ConfigProvider;

import java.nio.file.Path;

public class Config extends AbstractConfig {
    public static final ConfigProvider<Config> PROVIDER = ConfigProvider.of("conf.properties", Config::new);

    public final int fileIdLength = this.get("file-id-length", 6);
    public final String uploadsDirectory = this.get("upload-directory", "uploads");
    public final int serverPort = this.get("server-port", 42000);
    public final int maximumIdle = this.get("maximum-idle-ms", 120_000);
    public final int maximumFileSize = this.get("maximum-file-size-mb", -1); // TODO implement
    public final int maximumChunkSize = this.get("maximum-chunk-size-bytes", 2_097_152); // 2 MB

    public Config(String file) {
        super(file);
        store(Path.of(file), "");
    }
}
