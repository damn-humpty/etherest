package net.wizards.etherest.bot.util;

import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import net.wizards.etherest.bot.dom.Resources;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Bot {
    private static Resources res = Resources.get();

    private static final Pattern markerPatternCall = Pattern.compile("\\{([\\w\\.]+)\\.(\\w+)\\(\\);\\}");
    private static final Pattern markerPatternProp = Pattern.compile("\\{res\\.([\\w_]+);\\}");

    private static final Logger logger = LogManager.getLogger();
    private static final Marker TAG_CLASS = MarkerManager.getMarker(Bot.class.getSimpleName());

    private Bot() {
        throw new RuntimeException();
    }

    public static InlineKeyboardMarkup getInlineKeyboardMarkup(List<Map<String, String>> markup) {
        InlineKeyboardButton[][] keyboardButtons = markup.stream()
                .map(row -> row.entrySet().stream()
                        .map(e -> new InlineKeyboardButton(e.getValue()).callbackData(e.getKey()))
                        .collect(Collectors.toList())
                        .toArray(new InlineKeyboardButton[0]))
                .toArray(InlineKeyboardButton[][]::new);
        return new InlineKeyboardMarkup(keyboardButtons);
    }

    public static String replaceMarkers(String msg, String lng) {
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
                String replacement = res.str(matcher.group(2), lng);
                matcher.appendReplacement(sb2, replacement);
            }
        }
        matcher.appendTail(sb2);

        return sb2.toString();
    }

}
