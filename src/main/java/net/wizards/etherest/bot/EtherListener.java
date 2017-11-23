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
import net.wizards.etherest.bot.annotation.Reply;
import net.wizards.etherest.bot.dom.Client;
import net.wizards.etherest.bot.dom.Resources;
import net.wizards.etherest.bot.util.Bot;
import net.wizards.etherest.bot.util.Db;
import net.wizards.etherest.bot.util.Ethereum;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static net.wizards.etherest.util.Misc.nvl;

public class EtherListener implements UpdatesListener {
    private final TelegramBot bot;
    private Config cfg;
    private Resources res;
    private Set<Long> operators;

    private static Map<MappingKey, MappingValue> cmdWorkers = new HashMap<>();
    private static Map<MappingKey, MappingValue> cbWorkers = new HashMap<>();
    private static Map<MappingKey, MappingValue> replyWorkers = new HashMap<>();

    private static final Logger logger = LogManager.getLogger();
    private static final Marker TAG_CLASS = MarkerManager.getMarker(EtherBot.class.getSimpleName());

    EtherListener(TelegramBot bot) {
        this.bot = bot;
        cfg = Config.get();
        initWorkerMappings();
        res = Resources.get();
        operators = Db.getOperators();
    }

    private void initWorkerMappings() {
        for (final java.lang.reflect.Method method : getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(Command.class)) {
                Command cmd = method.getAnnotation(Command.class);
                for (String value : cmd.value()) {
                    cmdWorkers.put(new MappingKey(value), new MappingValue(method));
                }
            } else if (method.isAnnotationPresent(Callback.class)) {
                Callback callback = method.getAnnotation(Callback.class);
                for (String value : callback.value()) {
                    cbWorkers.put(new MappingKey(value), new MappingValue(method));
                }
            } else if (method.isAnnotationPresent(Reply.class)) {
                Reply reply = method.getAnnotation(Reply.class);
                for (Expect value : reply.value()) {
                    replyWorkers.put(new MappingKey(value), new MappingValue(method));
                }
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
                        Db.delClientExpect(client);
                        mappingValue.method.invoke(this, client, callbackQuery, query);
                    } else {
                        logger.info(TAG_CLASS, "Unknown callback: " + query);
                    }
                } else if (message != null) {
                    User user = message.from();
                    client = nvl(Db.readClient(user.id()), Client.from(user));
                    if (message.entities() != null) { // Command
                        Db.delClientExpect(client);
                        for (MessageEntity messageEntity : message.entities()) {
                            if (messageEntity.type() == MessageEntity.Type.bot_command) {
                                String cmd = message.text().substring(messageEntity.offset() + 1, messageEntity.length());
                                MappingValue mappingValue = cmdWorkers.get(new MappingKey(cmd));
                                if (mappingValue != null) {
                                    mappingValue.method.invoke(this, client, message);
                                } else {
                                    logger.info(TAG_CLASS, "Unknown value: " + cmd);
                                }
                            }
                        }
                    } else { // Reply
                        Expect expect = Db.getClientExpect(client);
                        Db.delClientExpect(client);
                        if (expect != null) {
                            MappingValue mappingValue = replyWorkers.get(new MappingKey(expect));
                            if (mappingValue != null) {
                                mappingValue.method.invoke(this, client, message);
                            }
                        } else {
                            logger.info(TAG_CLASS, "Unexpected reply: " + message.text());
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
    @Command("operator")
    private void operator(Client client, Message message) {
        String msgBody = res.str(client.getLangCode(), "operator_message");
        SendMessage request = new SendMessage(message.from().id(), msgBody)
                .parseMode(ParseMode.HTML)
                .disableWebPagePreview(false)
                .disableNotification(true);
        bot.execute(request);
        Db.setClientExpect(client, Expect.OPERATOR_PASSWORD);
    }

    @SuppressWarnings("unused")
    @Reply(Expect.OPERATOR_PASSWORD)
    private void operatorReply(Client client, Message message) {
        if (Objects.equals(cfg.getOperatorPassword(), message.text())) {
            Db.addOperator(message.chat().id());
            String msgBody = res.str(client.getLangCode(), "operator_mode_enabled");
            SendMessage request = new SendMessage(message.from().id(), msgBody)
                    .parseMode(ParseMode.HTML)
                    .disableWebPagePreview(false)
                    .disableNotification(true);
            bot.execute(request);
        }
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
    @Command("buy")
    private void buy(Client client, Message message) {
        if (Ethereum.isValidWalletId(client.getWalletId())) {
            Db.updateClaim(client, c -> c.setWalletId(client.getWalletId()));
            String msgBody = res.str(client.getLangCode(), "pay_system_message");
            SendMessage request = new SendMessage(message.from().id(), msgBody)
                    .parseMode(ParseMode.HTML)
                    .disableWebPagePreview(false)
                    .disableNotification(true)
                    .replyMarkup(Bot.getInlineKeyboardMarkup(res.kb(client.getLangCode(), "pay_systems")));
            bot.execute(request);
        }
    }

    @SuppressWarnings("unused")
    @Callback({"on_buy", "on_pay_back", "on_payment_request_back"})
    private void buy(Client client, CallbackQuery query, List<String> args) {
        if (Ethereum.isValidWalletId(client.getWalletId())) {
            String msgBody = res.str(client.getLangCode(), "pay_system_message");
            EditMessageText editMessageText =
                    new EditMessageText(query.message().chat().id(), query.message().messageId(), msgBody)
                            .parseMode(ParseMode.HTML)
                            .replyMarkup(Bot.getInlineKeyboardMarkup(res.kb(client.getLangCode(), "pay_systems")));
            bot.execute(editMessageText);
        }
    }

    @SuppressWarnings("unused")
    @Callback({"on_pay_bitcoin", "on_pay_ethereum", "on_pay_qiwi", "on_pay_sberbank", "on_pay_tinkoff", "on_pay_paypal"})
    private void payXXX(Client client, CallbackQuery query, List<String> args) {
        Db.updateClaim(client, c -> c.setPaySystem(args.get(0).substring(7)));
        final String lng = client.getLangCode();
        String msgBody = Bot.replaceMarkers(res.str(lng, args.get(0) + "_message")
                + res.str(lng, "payment_size_request"), lng);
        logger.debug(TAG_CLASS, "Msg body from resource (with markers replaced): " + msgBody);
        EditMessageText editMessageText =
                new EditMessageText(query.message().chat().id(), query.message().messageId(), msgBody)
                        .parseMode(ParseMode.HTML);
        bot.execute(editMessageText);
        Db.setClientExpect(client, Expect.PAY_AMOUNT);
    }

    @SuppressWarnings("unused")
    @Reply(Expect.PAY_AMOUNT)
    private void payAmountReply(Client client, Message message) {
        try {
            Double amount = Double.valueOf(message.text());
            if (Ethereum.isValidAmount(amount)) {
                Db.updateClaim(client, c -> c.setAmount(amount));
                String msgBody = res.str(client.getLangCode(), "payment_detail_message");
                logger.debug(TAG_CLASS, "Msg body from resource: " + msgBody);
                SendMessage request = new SendMessage(message.from().id(), msgBody)
                        .parseMode(ParseMode.HTML)
                        .disableWebPagePreview(false)
                        .disableNotification(true)
                        .replyMarkup(Bot.getInlineKeyboardMarkup(res.kb(client.getLangCode(), "payment_request")));
                bot.execute(request);
            }
        } catch (NumberFormatException e) {
            logger.error(TAG_CLASS, "Invalid amount: " + message.text());
        }
    }

    @SuppressWarnings("unused")
    @Callback("on_payment_request_confirm")
    private void paymentConfirm(Client client, CallbackQuery query, List<String> args) {
        String msgBody = res.str(client.getLangCode(), "payment_detail_message");
        logger.debug(TAG_CLASS, "Msg body from resource: " + msgBody);
        EditMessageText editMessageText =
                new EditMessageText(query.message().chat().id(), query.message().messageId(), msgBody)
                        .parseMode(ParseMode.HTML)
                        .replyMarkup(Bot.getInlineKeyboardMarkup(res.kb(client.getLangCode(), "payment")));
        bot.execute(editMessageText);
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
    @Callback({"on_settings", "on_wallet_back", "on_lang_back"})
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
    @Callback("on_settings_wallet")
    private void settingsWallet(Client client, CallbackQuery query, List<String> args) {
        String msgBody = String.format(res.str(client.getLangCode(), "wallet_message"),
                nvl(client.getWalletId(), res.str(client.getLangCode(), "no_wallet")));
        logger.debug(TAG_CLASS, "Msg body from resource: " + msgBody);
        EditMessageText editMessageText =
                new EditMessageText(query.message().chat().id(), query.message().messageId(), msgBody)
                        .parseMode(ParseMode.HTML)
                        .replyMarkup(Bot.getInlineKeyboardMarkup(res.kb(client.getLangCode(), "wallet")));
        bot.execute(editMessageText);
    }

    @SuppressWarnings("unused")
    @Callback("on_wallet_edit")
    private void walletChange(Client client, CallbackQuery query, List<String> args) {
        String msgBody = res.str(client.getLangCode(), "wallet_edit_message");
        EditMessageText editMessageText =
                new EditMessageText(query.message().chat().id(), query.message().messageId(), msgBody)
                        .parseMode(ParseMode.HTML);
        bot.execute(editMessageText);
        Db.setClientExpect(client, Expect.NEW_WALLET_ID);
    }

    @SuppressWarnings("unused")
    @Reply(Expect.NEW_WALLET_ID)
    private void newWalletIdReply(Client client, Message message) {
        if (Ethereum.isValidWalletId(message.text())) {
            client.setWalletId(message.text());
            settings(client, message);
        }
    }

    @SuppressWarnings("unused")
    @Callback({"on_lang_lang", "on_settings_lang"})
    private void langLang(Client client, CallbackQuery query, List<String> args) {
        if (args!= null && !args.isEmpty()) {
            client.setLangCode(args.get(1));
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
        private String value;

        private MappingKey(Object value) {
            this.value = value.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MappingKey that = (MappingKey) o;

            return value.equals(that.value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public String toString() {
            return "MappingKey{" +"value='" + value + '}';
        }
    }

    private static class MappingValue {
        private Method method;

        MappingValue(Method method) {
            this.method = method;
        }
    }

    public enum Expect {
        NEW_WALLET_ID,
        PAY_AMOUNT,
        OPERATOR_PASSWORD
    }
}
