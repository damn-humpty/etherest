package net.wizards.etherest.bot.dom;

import com.google.gson.Gson;
import net.wizards.etherest.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Resources {
    private Resource defaultRes;
    private Map<String, Resource> resources = new HashMap<>();

    private static final Logger logger = LogManager.getLogger();
    private static final Marker TAG_CLASS = MarkerManager.getMarker(Resources.class.getSimpleName());

    private static Resources instance;

    private Resources(String defaultLang) {
        final File jarFile = new File(getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
        final Pattern resourceFileFilter = Pattern.compile("^resource_([a-z]{2})\\.json$");

        if(jarFile.isFile()) {
            try (final JarFile jar = new JarFile(jarFile)) {
                final Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    final String name = entries.nextElement().getName();
                    final Matcher matcher = resourceFileFilter.matcher(name);
                    if (matcher.find()) {
                        String lng = matcher.group(1);
                        Resource resource = new Gson().fromJson(
                                new InputStreamReader(getClass().getResourceAsStream("/" + name),
                                        StandardCharsets.UTF_8), Resource.class);
                        resources.put(lng, resource);
                        if (lng.equals(defaultLang)) {
                            defaultRes = resource;
                        }
                        logger.info(TAG_CLASS, "File " + name + " loaded as resource \"" + matcher.group(1) + "\"");
                    }
                }
            } catch (IOException e) {
                logger.error(TAG_CLASS, "Error reading resources", e);
                throw new IllegalStateException("Can't initialize resource data");
            }
        }
    }

    public static synchronized Resources get() {
        if (instance == null) {
            instance = new Resources(Config.get().getDefaultLang());
        }
        return instance;
    }

    public String str(String lng, String key) {
        Resource res = resources.getOrDefault(lng, defaultRes);
        if (res != null) {
            return res.msg.getOrDefault(key, defaultRes != null ? defaultRes.msg.get(key) : "");
        }
        return "";
    }

    public List<Map<String, String>> kb(String lng, String key) {
        Resource res = resources.getOrDefault(lng, defaultRes);
        if (res != null) {
            return res.kb.getOrDefault(key, defaultRes != null ? defaultRes.kb.get(key) : new ArrayList<>());
        }
        return new ArrayList<>();
    }

    private static class Resource {
        private Map<String, String> msg;
        private Map<String, List<Map<String, String>>> kb;
    }
}
