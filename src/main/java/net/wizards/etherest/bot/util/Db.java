package net.wizards.etherest.bot.util;

import com.google.gson.Gson;
import net.wizards.etherest.Config;
import net.wizards.etherest.bot.dom.Client;
import net.wizards.etherest.database.Redis;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

public class Db {
    private static Redis redis = Redis.getInstance();
    private static Config cfg = Config.get();

    private static final Logger logger = LogManager.getLogger();
    private static final Marker TAG_CLASS = MarkerManager.getMarker(Db.class.getSimpleName());
    private static final Marker TAG_REDIS = MarkerManager.getMarker("REDIS");

    private Db() {
        throw new RuntimeException();
    }

    public static Client readClient(int id) {
        final String key = "client:" + id;
        Client client = redis.exists(key) ? new Gson().fromJson(redis.get(key), Client.class) : null;
        if (cfg.isLogRedisDataFlow()) {
            logger.debug(TAG_REDIS, "Client read from Redis: " + client);
        }
        return client;
    }

    public static void writeClient(Client client) {
        final String key = "client:" + client.getId();
        redis.set(key, new Gson().toJson(client), cfg.getClientDataExpiry());
        if (cfg.isLogRedisDataFlow()) {
            logger.debug(TAG_REDIS, "Client written to Redis: " + client);
        }
    }
}
