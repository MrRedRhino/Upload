package org.pipeman.upload.utils;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Iq80DBFactory;

import java.io.File;
import java.io.IOException;


public class Database {
    public static final Database INSTANCE = new Database();
    private final DB db;

    public Database() {
        Options options = new Options();
        options.createIfMissing(true);
        options.cacheSize(8_388_608); // 8 MB

        try {
            db = Iq80DBFactory.factory.open(new File("db"), options);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    public DB getDB() {
        return db;
    }

    public void stop() {
        try {
            db.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
