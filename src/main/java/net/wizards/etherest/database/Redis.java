package net.wizards.etherest.database;

import net.wizards.etherest.Config;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisConnectionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

public class Redis {
    private JedisPool pool;
    private String hostname = "";
    private int port = -1;
    private String password;
    private Config config;

    private static Redis instance;

    private static final Logger logger = LogManager.getLogger();
    private static final Marker TAG_CLASS = MarkerManager.getMarker(Redis.class.getSimpleName());
    private static final Marker TAG_REDIS = MarkerManager.getMarker("REDIS");

    private Redis() {
        logger.info(TAG_CLASS, "Starting Redis initialization");
        reconfig();
    }

    public static Redis getInstance() {
        if (instance == null) {
            instance = new Redis();
        }
        return instance;
    }

    private JedisPool getPool() {
        if (pool == null) {
            pool = new JedisPool(new GenericObjectPoolConfig(), hostname, port, 1800, password);

            Jedis jedis = null;
            try {
                jedis = pool.getResource();
            } catch (JedisConnectionException e) {
                if (password != null && e.getCause().getMessage().equals("ERR Client sent AUTH, but no password is set")) {
                    password = null;
                    destroyPool();
                    return getPool();
                } else {
                    throw e;
                }
            } finally {
                if (jedis != null) jedis.close();
            }
            logger.info(TAG_CLASS, "Connection pool created");
        }

        return pool;
    }

    private void destroyPool() {
        if (pool != null) {
            pool.destroy();
            pool = null;
            logger.info(TAG_CLASS, "Connection pool destroyed");
        }
    }

    public boolean testConnection() {
        try (Jedis jedis = getPool().getResource()) {
            jedis.exists("dummy");
            logger.info(TAG_REDIS, "Redis connection to " + hostname + ":" + port + " is valid");
            return true;
        } catch (Exception e) {
            logger.info(TAG_REDIS, "Redis connection to " + hostname + ":" + port + " is not valid", e);
            return false;
        }
    }

    public String get(String key) {
        try (Jedis jedis = getPool().getResource()) {
            String result = jedis.get(ns(key));
            if (config.isLogRedisDataFlow()) {
                logger.debug(TAG_REDIS, String.format("Get key %s data", key));
            }
            return result;
        }
    }

    public void set(String key, String value, int expireSeconds) {
        try (Jedis jedis = getPool().getResource()) {
            jedis.set(ns(key), value);
            if (expireSeconds > 0) {
                jedis.expire(ns(key), expireSeconds);
            }
            if (config.isLogRedisDataFlow()) {
                logger.debug(TAG_REDIS, String.format("Key %s stored for %d seconds", key, expireSeconds));
            }
        }
    }

    public void del(String key) {
        try (Jedis jedis = getPool().getResource()) {
            jedis.del(ns(key));
            if (config.isLogRedisDataFlow()) {
                logger.debug(TAG_REDIS, String.format("Key %s deleted", key));
            }
        }
    }

    public boolean exists(String key) {
        try (Jedis jedis = getPool().getResource()) {
            boolean result = jedis.exists(ns(key));
            if (config.isLogRedisDataFlow()) {
                logger.debug(TAG_REDIS, String.format("Key %s %s", key, result ? "exists" : "not exists"));
            }
            return result;
        }
    }

    private String ns(String key) {
        if (key.startsWith("etherest:")) {
            return key;
        }
        return "etherest:" + key;
    }

    private void reconfig() {
        config = Config.get();
        String hostnameTmp = config.getRedisHost();
        int portTmp = config.getRedisPort();
        String passwordTmp = config.getRedisPassword();
        if (port != -1) {
            if (!hostname.equals(hostnameTmp) || port != portTmp) {
                destroyPool();
                hostname = hostnameTmp;
                port = portTmp;
                password = passwordTmp;
                pool = getPool();
                logger.info(TAG_CLASS, "Redis connection re-created");
            }
        } else {
            hostname = hostnameTmp;
            port = portTmp;
            password = passwordTmp;
            pool = getPool();
            logger.info(TAG_CLASS, "Redis connection created");

        }
    }
}
