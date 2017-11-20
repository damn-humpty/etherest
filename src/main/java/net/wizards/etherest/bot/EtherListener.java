package net.wizards.etherest.bot;

import com.google.gson.Gson;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.MessageEntity;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.EditMessageText;
import com.pengrad.telegrambot.request.SendMessage;
import net.wizards.etherest.Config;
import net.wizards.etherest.bot.annotation.Callback;
import net.wizards.etherest.bot.annotation.Command;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EtherListener implements UpdatesListener {
    private final TelegramBot bot;
    private Config cfg;
    private static Map<MappingKey, MappingValue> cmdWorkers = new HashMap<>();
    private static Map<MappingKey, MappingValue> cbWorkers = new HashMap<>();

    private static final Pattern markerPattern = Pattern.compile("\\{([\\w\\.]+)\\.(\\w+)\\(\\);\\}");

    private static final Logger logger = LogManager.getLogger();
    private static final Marker TAG_CLASS = MarkerManager.getMarker(EtherBot.class.getSimpleName());

    private static Map<String, String> langMap = new HashMap<>();
    static {
        langMap.put("en", new String(new int[] {0x1F1EC, 0x1F1E7}, 0, 2) + "ENG");
        langMap.put("ru", new String(new int[] {0x1F1F7, 0x1F1FA}, 0, 2) + "РУС");
    }

    EtherListener(TelegramBot bot) {
        this.bot = bot;
        cfg = Config.get();
        initWorkerMappings();
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
                        mappingValue.method.invoke(this, callbackQuery, query.subList(1, query.size()));
                    } else {
                        logger.info(TAG_CLASS, "Unknown callback: " + query);
                    }
                } else if (message != null && message.entities() != null) {
                    for (MessageEntity messageEntity : message.entities()) {
                        if (messageEntity.type() == MessageEntity.Type.bot_command) {
                            String cmd = message.text().substring(messageEntity.offset() + 1, messageEntity.length());
                            MappingValue mappingValue = cmdWorkers.get(new MappingKey(cmd));
                            if (mappingValue != null) {
                                mappingValue.method.invoke(this, message);
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

/*
                String[] callbackData = callbackQuery.data().split(" ");
                switch (callbackData[0]) {
                    case "lang":
                        EditMessageText editMessageText =
                                new EditMessageText(
                                        callbackQuery.message().chat().id(),
                                        callbackQuery.message().messageId(),
                                        "Current language set to: " + callbackData[1]);
                        bot.execute(editMessageText);
                        break;
                    default:
                        break;
                }
 */

/*
                User user = message.from();
                Chat chat = message.chat();
                Instant date = Instant.ofEpochSecond(message.date());
                String text = message.text();
                for (MessageEntity messageEntity : message.entities()) {
                    if (messageEntity.type() == MessageEntity.Type.bot_command) {
                        switch (text.substring(messageEntity.offset(), messageEntity.length())) {
                            case "/lang1":
                                System.out.println("/lang");
                                SendMessage request = new SendMessage(chat.id(), "Select language")
                                        .parseMode(ParseMode.HTML)
                                        //.disableWebPagePreview(true)
                                        //.disableNotification(true)
                                        //.replyToMessageId(message.messageId())
                                        .replyMarkup(new ReplyKeyboardMarkup(
                                                new String[]{"RUS", "ENG"})
                                                .oneTimeKeyboard(true)   // optional
                                                .resizeKeyboard(true)    // optional
                                                .selective(true));
                                SendResponse sendResponse = bot.execute(request);
                                break;
                            case "/lang":
                                SendMessage request1 = new SendMessage(message.from().id(),
                                        "Current language: " + langMap.get(user.languageCode()))
                                        .parseMode(ParseMode.HTML)
                                        .disableWebPagePreview(false)
                                        .disableNotification(true)
                                        .replyMarkup(getInlineKeyboardMarkup(langMap));
                                bot.execute(request1);
                                break;
                            default:
                                System.out.println("Unknown command");
                                break;
                        }
                    }
                }
            }
 */

    @SuppressWarnings("unused")
    @Command("lang")
    private void langCommand(Message message) {
        SendMessage request = new SendMessage(message.from().id(),
                "Current language: " + langMap.get(message.from().languageCode()))
                .parseMode(ParseMode.HTML)
                .disableWebPagePreview(false)
                .disableNotification(true)
                .replyMarkup(getInlineKeyboardMarkup(langMap));
        bot.execute(request);
    }

    @SuppressWarnings("unused")
    @Callback("lang")
    private void langCallback(CallbackQuery query, List<String> args) {
        EditMessageText editMessageText =
                new EditMessageText(
                        query.message().chat().id(),
                        query.message().messageId(),
                        "Current language set to: " + langMap.get(args.get(0)));
        bot.execute(editMessageText);
    }

    @SuppressWarnings("unused")
    @Command("debug")
    private void debugCommand(Message message) {
        String msg = "Current exchange rate is {net.wizards.etherest.bot.EnvVariables.getBtc2Eth();}.";
        SendMessage request = new SendMessage(message.chat().id(), replaceMarkers(msg))
                .parseMode(ParseMode.HTML)
                .disableWebPagePreview(false)
                .disableNotification(true);
        bot.execute(request);
    }

    private String replaceMarkers(String msg) {
        Matcher matcher = markerPattern.matcher(msg);
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
        return sb.toString();
    }

    private InlineKeyboardMarkup getInlineKeyboardMarkup(Map<String, String> mMap) {
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
