package net.wizards.etherest.bot;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.*;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.EditMessageText;
import com.pengrad.telegrambot.request.SendMessage;
import net.wizards.etherest.Config;
import net.wizards.etherest.bot.annotation.Callback;
import net.wizards.etherest.bot.annotation.Command;
import net.wizards.etherest.bot.dom.Client;
import net.wizards.etherest.database.Redis;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.wizards.etherest.util.Misc.nvl;

public class EtherListener implements UpdatesListener {
    private final TelegramBot bot;
    private Config cfg;
    private Redis redis;

    private static Type typeMap = new TypeToken<HashMap<String, String>>(){}.getType();

    private static Map<MappingKey, MappingValue> cmdWorkers = new HashMap<>();
    private static Map<MappingKey, MappingValue> cbWorkers = new HashMap<>();

    private static final Pattern markerPatternCall = Pattern.compile("\\{([\\w\\.]+)\\.(\\w+)\\(\\);\\}");
    private static final Pattern markerPatternProp = Pattern.compile("\\{([\\w_]+)\\.([\\w_]+);\\}");

    private static final Logger logger = LogManager.getLogger();
    private static final Marker TAG_CLASS = MarkerManager.getMarker(EtherBot.class.getSimpleName());

    EtherListener(TelegramBot bot) {
        this.bot = bot;
        cfg = Config.get();
        initWorkerMappings();
        redis = Redis.getInstance();
    }

    private void initWorkerMappings() {
        for (final java.lang.reflect.Method method : getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(Command.class)) {
                Command cmd = method.getAnnotation(Command.class);
                cmdWorkers.put(new MappingKey(cmd.value()), new MappingValue(method));
            } else if (method.isAnnotationPresent(Callback.class)) {
                Callback callback = method.getAnnotation(Callback.class);
                cbWorkers.put(new MappingKey(callback.value()), new MappingValue(method));
            }
        }
    }

    @Override
    public int process(List<Update> updates) {
        Gson gson = new Gson();
        for (Update update : updates) {
            if (cfg.isLogBotRequests()) {
                logger.info(TAG_CLASS, "Processing request: " + gson.toJson(update));
            }

            Message message = update.message();
            CallbackQuery callbackQuery = update.callbackQuery();

            try {
                if (callbackQuery != null) {
                    List<String> query = Arrays.asList(callbackQuery.data().split(" "));
                    MappingValue mappingValue = cbWorkers.get(new MappingKey(query.get(0)));
                    if (mappingValue != null) {
                        User user = callbackQuery.from();
                        Client client = nvl(readClient(user.id()), Client.from(user));
                        mappingValue.method.invoke(this, client, callbackQuery, query.subList(1, query.size()));
                    } else {
                        logger.info(TAG_CLASS, "Unknown callback: " + query);
                    }
                } else if (message != null && message.entities() != null) {
                    for (MessageEntity messageEntity : message.entities()) {
                        if (messageEntity.type() == MessageEntity.Type.bot_command) {
                            String cmd = message.text().substring(messageEntity.offset() + 1, messageEntity.length());
                            MappingValue mappingValue = cmdWorkers.get(new MappingKey(cmd));
                            if (mappingValue != null) {
                                User user = message.from();
                                Client client = nvl(readClient(user.id()), Client.from(user));
                                mappingValue.method.invoke(this, client, message);
                            } else {
                                logger.info(TAG_CLASS, "Unknown command: " + cmd);
                            }
                        }
                    }
                }
            } catch (InvocationTargetException e) {
                logger.error(TAG_CLASS, "Internal exception", e);
            } catch (Exception e) {
                logger.error(TAG_CLASS, "Reflexive call failed", e);
            }
        }

        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    private Client readClient(int id) {
        final String key = "client:" + id;
        return redis.exists(key) ? new Gson().fromJson(redis.get(key), Client.class) : null;
    }

    private void writeClient(Client client) {
        final String key = "client:" + client.getId();
        redis.set(key, new Gson().toJson(client), cfg.getClientDataExpiry());
    }

    @SuppressWarnings("unused")
    @Command("lang")
    private void langCommand(Client client, Message message) {
        ResourceBundle resourceBundle = ResourceBundle.getBundle("strings", new Locale(client.getLangCode()));
        String msgBody = resourceBundle.getString("lang_message");
        SendMessage request = new SendMessage(message.from().id(), msgBody)
                .parseMode(ParseMode.HTML)
                .disableWebPagePreview(false)
                .disableNotification(true)
                .replyMarkup(getInlineKeyboardMarkup(resourceBundle.getString("lang_keyboard")));
        bot.execute(request);
    }

    @SuppressWarnings("unused")
    @Callback("lang")
    private void langCallback(Client client, CallbackQuery query, List<String> args) {
        client.setLangCode(args.get(0));
        writeClient(client);
        ResourceBundle resourceBundle = ResourceBundle.getBundle("strings", new Locale(client.getLangCode()));
        String msgBody = resourceBundle.getString("lang_message");
        EditMessageText editMessageText =
                new EditMessageText(query.message().chat().id(), query.message().messageId(), msgBody)
                        .parseMode(ParseMode.HTML);
        bot.execute(editMessageText);
    }

    @SuppressWarnings("unused")
    @Command("debug")
    private void debugCommand(Client client, Message message) {
        String msg = "Current exchange rate is {net.wizards.etherest.bot.EnvVariables.getBtc2Eth();}.";
        SendMessage request = new SendMessage(message.chat().id(), replaceMarkers(msg))
                .parseMode(ParseMode.HTML)
                .disableWebPagePreview(false)
                .disableNotification(true);
        bot.execute(request);
    }

    private String replaceMarkers(String msg) {
        return replaceMarkers(msg, Locale.getDefault());
    }

    private String replaceMarkers(String msg, Locale chatLocale) {
        Matcher matcher = markerPatternCall.matcher(msg);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            if (matcher.groupCount() == 2) {
                try {
                    Class<?> clazz = Class.forName(matcher.group(1));
                    Method method = clazz.getMethod(matcher.group(2));
                    String replacement = (String) method.invoke(null);
                    if (replacement == null) {
                        replacement = "";
                    }
                    matcher.appendReplacement(sb, replacement);
                } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    logger.error(TAG_CLASS, "Error replacing marker " + matcher.group(0), e);
                }
            }
        }
        matcher.appendTail(sb);

        matcher = markerPatternProp.matcher(sb.toString());
        StringBuffer sb2 = new StringBuffer();
        while (matcher.find()) {
            if (matcher.groupCount() == 2) {
                try {
                    String replacement = ResourceBundle.getBundle(matcher.group(1), chatLocale).getString(matcher.group(2));
                    matcher.appendReplacement(sb2, replacement);
                } catch (MissingResourceException e) {
                    logger.error(TAG_CLASS, "Error replacing marker " + matcher.group(0), e);
                }
            }
        }
        matcher.appendTail(sb2);

        return sb2.toString();
    }

    private InlineKeyboardMarkup getInlineKeyboardMarkup(String mapSerialized) {
        Map<String, String> mMap = new Gson().fromJson(mapSerialized, typeMap);
        return new InlineKeyboardMarkup(mMap.entrySet().stream()
                .map(e -> new InlineKeyboardButton(e.getValue()).callbackData("lang " + e.getKey()))
                .collect(Collectors.toList())
                .toArray(new InlineKeyboardButton[0]));
    }

    private static class MappingKey {
        private String command;

        private MappingKey(String command) {
            this.command = command;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MappingKey that = (MappingKey) o;

            return command.equals(that.command);
        }

        @Override
        public int hashCode() {
            return command.hashCode();
        }

        @Override
        public String toString() {
            return "MappingKey{" +"command='" + command + '}';
        }
    }

    private static class MappingValue {
        private Method method;

        MappingValue(Method method) {
            this.method = method;
        }
    }

}
