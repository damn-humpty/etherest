package net.wizards.etherest.bot;

import com.google.gson.Gson;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.*;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.EditMessageText;
import com.pengrad.telegrambot.request.SendMessage;
import net.wizards.etherest.Config;
import net.wizards.etherest.bot.annotation.Callback;
import net.wizards.etherest.bot.annotation.Command;
import net.wizards.etherest.bot.dom.Client;
import net.wizards.etherest.bot.dom.Resources;
import net.wizards.etherest.bot.util.Bot;
import net.wizards.etherest.bot.util.Db;
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

import static net.wizards.etherest.util.Misc.nvl;

public class EtherListener implements UpdatesListener {
    private final TelegramBot bot;
    private Config cfg;
    private Resources res;

    private static Map<MappingKey, MappingValue> cmdWorkers = new HashMap<>();
    private static Map<MappingKey, MappingValue> cbWorkers = new HashMap<>();

    private static final Logger logger = LogManager.getLogger();
    private static final Marker TAG_CLASS = MarkerManager.getMarker(EtherBot.class.getSimpleName());

    EtherListener(TelegramBot bot) {
        this.bot = bot;
        cfg = Config.get();
        initWorkerMappings();

        res = Resources.get();
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
                Client client = null;
                if (callbackQuery != null) {
                    List<String> query = Arrays.asList(callbackQuery.data().split(" "));
                    MappingValue mappingValue = cbWorkers.get(new MappingKey(query.get(0)));
                    if (mappingValue != null) {
                        User user = callbackQuery.from();
                        client = nvl(Db.readClient(user.id()), Client.from(user));
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
                                client = nvl(Db.readClient(user.id()), Client.from(user));
                                mappingValue.method.invoke(this, client, message);
                            } else {
                                logger.info(TAG_CLASS, "Unknown command: " + cmd);
                            }
                        }
                    }
                }
                if (client != null && client.isModified()) {
                    Db.writeClient(client);
                }
            } catch (InvocationTargetException e) {
                logger.error(TAG_CLASS, "Internal exception", e);
            } catch (Exception e) {
                logger.error(TAG_CLASS, "Reflexive call failed", e);
            }
        }

        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    @SuppressWarnings("unused")
    @Command("lang")
    private void langCommand(Client client, Message message) {
        String msgBody = res.str(client.getLangCode(), "lang_message");
        SendMessage request = new SendMessage(message.from().id(), msgBody)
                .parseMode(ParseMode.HTML)
                .disableWebPagePreview(false)
                .disableNotification(true)
                .replyMarkup(Bot.getInlineKeyboardMarkup(res.kb(client.getLangCode(), "lang")));
        bot.execute(request);
    }

    @SuppressWarnings("unused")
    @Command("settings")
    private void settings(Client client, Message message) {
        String msgBody = String.format(res.str(client.getLangCode(), "settings_message"),
                nvl(client.getWalletId(), res.str(client.getLangCode(), "no_wallet")));
        logger.debug(TAG_CLASS, "Msg body from resource: " + msgBody);
        SendMessage request = new SendMessage(message.from().id(), msgBody)
                .parseMode(ParseMode.HTML)
                .disableWebPagePreview(false)
                .disableNotification(true)
                .replyMarkup(Bot.getInlineKeyboardMarkup(res.kb(client.getLangCode(), "settings")));
        bot.execute(request);
    }

    @SuppressWarnings("unused")
    @Callback("on_settings")
    private void settings(Client client, CallbackQuery query, List<String> args) {
        String msgBody = String.format(res.str(client.getLangCode(), "settings_message"),
                nvl(client.getWalletId(), res.str(client.getLangCode(), "no_wallet")));
        logger.debug(TAG_CLASS, "Msg body from resource: " + msgBody);
        EditMessageText editMessageText =
                new EditMessageText(query.message().chat().id(), query.message().messageId(), msgBody)
                        .parseMode(ParseMode.HTML)
                        .replyMarkup(Bot.getInlineKeyboardMarkup(res.kb(client.getLangCode(), "settings")));
        bot.execute(editMessageText);
    }

    @SuppressWarnings("unused")
    @Callback("on_settings_lang")
    private void settingsLang(Client client, CallbackQuery query, List<String> args) {
        langLang(client, query, args);
    }

    @SuppressWarnings("unused")
    @Callback("on_settings_wallet")
    private void settingsWallet(Client client, CallbackQuery query, List<String> args) {
        if (args!= null && !args.isEmpty()) {
            client.setLangCode(args.get(0));
        }
        String msgBody = String.format(res.str(client.getLangCode(), "wallet_message"),
                nvl(client.getWalletId(), res.str(client.getLangCode(), "no_wallet")));
        EditMessageText editMessageText =
                new EditMessageText(query.message().chat().id(), query.message().messageId(), msgBody)
                        .parseMode(ParseMode.HTML)
                        .replyMarkup(Bot.getInlineKeyboardMarkup(res.kb(client.getLangCode(), "wallet")));
        bot.execute(editMessageText);
    }

    @SuppressWarnings("unused")
    @Callback("on_wallet_back")
    private void walletBack(Client client, CallbackQuery query, List<String> args) {
        settings(client, query, args);
    }

    @SuppressWarnings("unused")
    @Callback("on_lang_back")
    private void langBack(Client client, CallbackQuery query, List<String> args) {
        settings(client, query, args);
    }

    @SuppressWarnings("unused")
    @Callback("on_lang_lang")
    private void langLang(Client client, CallbackQuery query, List<String> args) {
        if (args!= null && !args.isEmpty()) {
            client.setLangCode(args.get(0));
        }
        String msgBody = res.str(client.getLangCode(), "lang_message");
        EditMessageText editMessageText =
                new EditMessageText(query.message().chat().id(), query.message().messageId(), msgBody)
                        .parseMode(ParseMode.HTML)
                        .replyMarkup(Bot.getInlineKeyboardMarkup(res.kb(client.getLangCode(), "lang")));
        bot.execute(editMessageText);
    }

    @SuppressWarnings("unused")
    @Command("debug")
    private void debug(Client client, Message message) {
        String msg = "Current exchange rate is {net.wizards.etherest.bot.EnvVariables.getBtc2Eth();}.";
        SendMessage request = new SendMessage(message.chat().id(), Bot.replaceMarkers(msg, client.getLangCode()))
                .parseMode(ParseMode.HTML)
                .disableWebPagePreview(false)
                .disableNotification(true);
        bot.execute(request);
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
