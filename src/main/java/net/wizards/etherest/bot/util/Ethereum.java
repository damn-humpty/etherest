package net.wizards.etherest.bot.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

public class Ethereum {
    private static final Logger logger = LogManager.getLogger();
    private static final Marker TAG_CLASS = MarkerManager.getMarker(Ethereum.class.getSimpleName());

    private Ethereum() {
        throw new RuntimeException();
    }

    public static boolean isValidWalletId(String walletId) {
        return walletId != null && !walletId.isEmpty();
    }

    public static boolean isValidAmount(Double amount) {
        return amount != null && amount > 0;
    }
}
