package me.redteapot.rebot.data;

import lombok.extern.slf4j.Slf4j;
import me.redteapot.rebot.Config;
import me.redteapot.rebot.data.serde.SnowflakeSerde;
import org.dizitart.no2.Nitrite;
import org.dizitart.no2.objects.ObjectRepository;

import static me.redteapot.rebot.Checks.ensure;

@Slf4j
public final class Database {
    private static Database instance;

    private final Nitrite nitrite;

    private Database(Config.Database config) {
        this.nitrite = Nitrite.builder()
            .registerModule(new SnowflakeSerde())
            .filePath(config.getFile())
            .openOrCreate();
    }

    public static <T> ObjectRepository<T> getRepository(Class<T> type) {
        return getInstance().nitrite.getRepository(type);
    }

    public static Nitrite getNitrite() {
        return getInstance().nitrite;
    }

    private static Database getInstance() {
        ensure(instance != null, "Requested Database instance before init");
        return instance;
    }

    public static void init(Config.Database config) {
        instance = new Database(config);
    }

    public static void close() {
        getInstance().nitrite.close();
    }
}
