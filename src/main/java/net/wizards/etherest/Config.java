package net.wizards.etherest;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static net.wizards.etherest.util.Misc.nvl;

public class Config {
    private Integer portRestListener;
    private Integer restListenerPoolSize;

    private String botToken;
    private String botName;
    private String botUsername;

    private String supportedLang;
    private String defaultLang;

    private String operatorChats;

    private String buildNumber;

    private Boolean logRedisDataFlow;
    private Boolean logHttpRequests;
    private Boolean logHttpResponses;
    private Boolean logRestRequests;
    private Boolean logRestResponses;
    private Boolean logRestConnectedClient;
    private Boolean logBotRequests;
    private Boolean logBotMessages;

    private String redisHost;
    private Integer redisPort;
    private Integer clientDataExpiry;
    private String redisPassword;

    private String trustedIp;

    transient private static Config instance;

    private static final Logger logger = LogManager.getLogger();
    private static final Marker TAG_CLASS = MarkerManager.getMarker(Config.class.getSimpleName());

    public Integer getClientDataExpiry() {
        return clientDataExpiry;
    }

    public String getDefaultLang() {
        return defaultLang;
    }

    public String getSupportedLang() {
        return supportedLang;
    }

    Integer getPortRestListener() {
        return portRestListener;
    }

    public Boolean isLogRestResponses() {
        return logRestResponses;
    }

    Integer getRestListenerPoolSize() {
        return restListenerPoolSize;
    }

    public String getOperatorChats() {
        return operatorChats;
    }

    String getTrustedIp() {
        return trustedIp;
    }

    private Config(String src) {
        setConfig(src);
    }

    public String getBotToken() {
        return botToken;
    }

    public String getBotName() {
        return botName;
    }

    public String getBotUsername() {
        return botUsername;
    }

    String getBuildNumber() {
        return buildNumber;
    }

    public Boolean isLogRedisDataFlow() {
        return logRedisDataFlow;
    }

    public Boolean isLogHttpRequests() {
        return logHttpRequests;
    }

    public Boolean isLogHttpResponses() {
        return logHttpResponses;
    }

    Boolean isLogRestRequests() {
        return logRestRequests;
    }

    public Boolean isLogRestConnectedClient() {
        return logRestConnectedClient;
    }

    public Boolean isLogBotRequests() {
        return logBotRequests;
    }

    public Boolean isLogBotMessages() {
        return logBotMessages;
    }

    public String getRedisHost() {
        return redisHost;
    }

    public int getRedisPort() {
        return redisPort;
    }

    public String getRedisPassword() {
        return redisPassword;
    }

    public static synchronized Config get() {
        if (instance == null) {
            instance = new Config(null);
        }
        return instance;
    }

    private void setFrom(Config config) {
        portRestListener = nvl(config.getPortRestListener(), portRestListener);
        restListenerPoolSize = nvl(config.getRestListenerPoolSize(), restListenerPoolSize);

        botToken = nvl(config.getBotToken(), botToken);
        botName = nvl(config.getBotName(), botName);
        botUsername = nvl(config.getBotUsername(), botUsername);

        supportedLang = nvl(config.getSupportedLang(), supportedLang);
        defaultLang = nvl(config.getDefaultLang(), defaultLang);

        operatorChats = nvl(config.getOperatorChats(), operatorChats);

        logRedisDataFlow = nvl(config.isLogRedisDataFlow(), logRedisDataFlow);
        logHttpRequests = nvl(config.isLogHttpRequests(), logHttpRequests);
        logHttpResponses = nvl(config.isLogHttpResponses(), logHttpResponses);
        logRestRequests = nvl(config.isLogRestRequests(), logRestRequests);
        logRestResponses = nvl(config.isLogRestResponses(), logRestResponses);
        logRestConnectedClient = nvl(config.isLogRestConnectedClient(), logRestConnectedClient);
        logBotMessages = nvl(config.isLogBotMessages(), logBotMessages);
        logBotRequests = nvl(config.isLogBotRequests(), logBotRequests);

        redisHost = nvl(config.getRedisHost(), redisHost);
        redisPort = nvl(config.getRedisPort(), redisPort);
        clientDataExpiry = nvl(config.getClientDataExpiry(), clientDataExpiry);
        redisPassword = nvl(config.getRedisPassword(), redisPassword);

        trustedIp = nvl(config.getTrustedIp(), trustedIp);

        buildNumber = nvl(config.getBuildNumber(), buildNumber);
    }

    public void setConfig(String fileName) {
        if (fileName == null) {
            try {
                setFrom(new Gson().fromJson(
                        new InputStreamReader(getClass().getResourceAsStream("/etherest_cfg.json"),
                                StandardCharsets.UTF_8), Config.class));
            } catch (Exception e) {
                logger.error(TAG_CLASS, "Error setting built-in configuration", e);
                throw new RuntimeException("Fallback parameters are corrupted or missing");
            }
        } else {
            try {
                setFrom(new Gson().fromJson(new InputStreamReader(
                        new FileInputStream(fileName), StandardCharsets.UTF_8), Config.class));
            } catch (Exception e) {
                logger.error(TAG_CLASS, "Error setting external configuration", e);
                logger.info(TAG_CLASS, "Effective parameters: " + new Gson().toJson(this));
                throw new RuntimeException("Configuration exception: config corrupted or not found");
            }
        }
    }

}
