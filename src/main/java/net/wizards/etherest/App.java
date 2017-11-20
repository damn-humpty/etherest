package net.wizards.etherest;

import net.wizards.etherest.bot.EtherBot;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class App {
    private static App instance;
    private static String configFile;
    private static String pidFile;

    private static final Logger logger = LogManager.getLogger();
    private static final Marker TAG_CLASS = MarkerManager.getMarker(App.class.getSimpleName());

    public static void main(String[] args) throws Exception {
        processParams(args);
        writePid();
        Config.get().setConfig(configFile);
        getApp().run();
    }

    private void run() throws Exception {
        new Thread(RestService.getInstance()).start();
        EtherBot.get().run();
    }

    static App getApp() {
        if (instance == null) {
            instance = new App();
        }
        return instance;
    }

    private App() {}

    private static void processParams(String[] args) {
        Options options = new Options();

        Option input = new Option("p", "pid", true, "pid file full path");
        input.setRequired(true);
        options.addOption(input);

        Option output = new Option("c", "config", true, "config file full path");
        output.setRequired(true);
        options.addOption(output);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("etherest", options);
            System.exit(1);
        }

        configFile = cmd.getOptionValue("config");
        pidFile = cmd.getOptionValue("pid");
    }

    private static void writePid() {
        String processName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
        try (FileOutputStream pf = new FileOutputStream(pidFile)) {
            pf.write(processName.split("@")[0].getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            logger.error(TAG_CLASS, "Can't write pid file", e);
        }
    }

    void removePidFile() {
        try {
            Files.delete(Paths.get(pidFile));
        } catch (IOException e) {
            logger.trace(TAG_CLASS, "Error removing pid file");
        }
    }
}
