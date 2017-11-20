package net.wizards.etherest.bot;


import com.pengrad.telegrambot.TelegramBot;
import net.wizards.etherest.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

public class EtherBot {
    private static EtherBot instance;
    private static TelegramBot bot;
    private static Config cfg = Config.get();

    private static final Logger logger = LogManager.getLogger();
    private static final Marker TAG_CLASS = MarkerManager.getMarker(EtherBot.class.getSimpleName());

    private EtherBot(String botToken){
        bot = new TelegramBot(botToken);
    }

    public static synchronized EtherBot get() {
        if (instance == null) {
            instance = new EtherBot(cfg.getBotToken());
        }
        return instance;
    }

    @Override
    public String toString() {
        return "{"
                + "\"name\": \""+ cfg.getBotName() + "\""
                + ",\"userName\": \""+ cfg.getBotUsername() + "\""
                + ",\"token\": \""+ cfg.getBotToken() + "\""
                + "}";
    }

    public void run() {
        bot.setUpdatesListener(new EtherListener(bot));
        logger.info(TAG_CLASS, "Bot started: " + this);
    }

    public void shutdown() {
        bot.removeGetUpdatesListener();
        logger.info(TAG_CLASS, "Bot stopped: " + this);
    }
}
