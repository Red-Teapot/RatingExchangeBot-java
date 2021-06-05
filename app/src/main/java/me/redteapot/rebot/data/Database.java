package me.redteapot.rebot.data;

import lombok.extern.slf4j.Slf4j;
import me.redteapot.rebot.Config;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import static me.redteapot.rebot.Checks.ensure;

@Slf4j
public final class Database {
    private static Database instance;

    private final EntityManagerFactory factory;

    private Database(Config.Database config) {
        factory = Persistence.createEntityManagerFactory("REBot");
    }

    public <T> EntityManager getEntityManager(Class<T> clazz) {
        return factory.createEntityManager();
    }

    public static Database getInstance() {
        ensure(instance != null, "Requested Database instance before init");
        return instance;
    }

    public static void init(Config.Database config) {
        instance = new Database(config);
    }

    public static void close() {
        getInstance().factory.close();
    }
}
