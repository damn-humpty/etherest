package net.wizards.etherest.bot.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.wizards.etherest.Config;
import net.wizards.etherest.bot.EtherListener;
import net.wizards.etherest.bot.dom.Client;
import net.wizards.etherest.bot.dom.PaymentClaim;
import net.wizards.etherest.database.Redis;
import net.wizards.etherest.util.Misc;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class Db {
    private static Redis redis = Redis.getInstance();
    private static Config cfg = Config.get();

    private static final Logger logger = LogManager.getLogger();
    private static final Marker TAG_CLASS = MarkerManager.getMarker(Db.class.getSimpleName());
    private static final Marker TAG_REDIS = MarkerManager.getMarker("REDIS");

    private static final String CLIENT_KEY_PREFIX = "client:";
    private static final String EXPECT_KEY_SUFFIX = ":expect";
    private static final String CLAIM_KEY_SUFFIX = ":claim";
    private static final String OPERATOR_LIST_KEY = "operators";

    private static final Type setType = new TypeToken<Set<Long>>(){}.getType();

    private static final int EXPECT_EXPIRY = 86400;
    private static final int CLAIM_EXPIRY = 86400;

    private Db() {
        throw new RuntimeException();
    }

    public static Client readClient(int id) {
        final String key = CLIENT_KEY_PREFIX + id;
        try {
            Client client = new Gson().fromJson(redis.get(key), Client.class);
            if (cfg.isLogRedisDataFlow()) {
                logger.debug(TAG_REDIS, "Client read from Redis: " + client);
            }
            return client;
        } catch (Exception e) {
            return null;
        }
    }

    public static void writeClient(Client client) {
        final String key = CLIENT_KEY_PREFIX + client.getId();
        redis.set(key, new Gson().toJson(client), cfg.getClientDataExpiry());
        if (cfg.isLogRedisDataFlow()) {
            logger.debug(TAG_REDIS, "Client written to Redis: " + client);
        }
    }

    public static void setClientExpect(Client client, EtherListener.Expect expect) {
        final String key = CLIENT_KEY_PREFIX + client.getId() + EXPECT_KEY_SUFFIX;
        redis.set(key, new Gson().toJson(expect), EXPECT_EXPIRY);
        if (cfg.isLogRedisDataFlow()) {
            logger.debug(TAG_REDIS, "Expect (" + expect + ") written to Redis for " + client);
        }
    }

    public static void delClientExpect(Client client) {
        final String key = CLIENT_KEY_PREFIX + client.getId() + EXPECT_KEY_SUFFIX;
        redis.del(key);
        if (cfg.isLogRedisDataFlow()) {
            logger.debug(TAG_REDIS, "Expect cleared in Redis for " + client);
        }
    }

    public static EtherListener.Expect getClientExpect(Client client) {
        final String key = CLIENT_KEY_PREFIX + client.getId() + EXPECT_KEY_SUFFIX;
        EtherListener.Expect expect = new Gson().fromJson(redis.get(key), EtherListener.Expect.class);
        if (cfg.isLogRedisDataFlow()) {
            logger.debug(TAG_REDIS, "Expect (" + expect + ") read from Redis for " + client);
        }
        return expect;
    }

    public static Set<Long> getOperators() {
        try {
            Set<Long> operators = Misc.nvl(new Gson().fromJson(redis.get(OPERATOR_LIST_KEY), setType), new HashSet<>());
            if (cfg.isLogRedisDataFlow()) {
                logger.debug(TAG_REDIS, "Operator chat list read from Redis: " + operators);
            }
            return operators;
        } catch (Exception e) {
            return new HashSet<>();
        }
    }

    public static void addOperator(long chatId) {
        Set<Long> operators = getOperators();
        operators.add(chatId);
        redis.set(OPERATOR_LIST_KEY, new Gson().toJson(operators), cfg.getClientDataExpiry());
        if (cfg.isLogRedisDataFlow()) {
            logger.debug(TAG_REDIS, "Operator list written to Redis: " + operators);
        }
    }

    public static PaymentClaim getClaim(Client client) {
        try {
            final String key = CLIENT_KEY_PREFIX + client.getId() + CLAIM_KEY_SUFFIX;
            PaymentClaim claim = Misc.nvl(new Gson().fromJson(redis.get(key), PaymentClaim.class), new PaymentClaim());
            if (cfg.isLogRedisDataFlow()) {
                logger.debug(TAG_REDIS, "Payment claim read from Redis: " + claim);
            }
            return claim;
        } catch (Exception e) {
            return new PaymentClaim();
        }
    }

    public static void updateClaim(Client client, Consumer<PaymentClaim> setter) {
        PaymentClaim claim = getClaim(client);
        setter.accept(claim);
        final String key = CLIENT_KEY_PREFIX + client.getId() + CLAIM_KEY_SUFFIX;
        redis.set(key, new Gson().toJson(claim), CLAIM_EXPIRY);
        if (cfg.isLogRedisDataFlow()) {
            logger.debug(TAG_REDIS, "Payment claim updated: " + claim);
        }
    }
}
