package net.wizards.etherest.util;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Base64;

public class Misc {
    public static byte[] hexToBytes(String str) {
        if (str == null) {
            return null;
        } else if (str.length() < 2) {
            return null;
        } else {
            int len = str.length() / 2;
            byte[] buffer = new byte[len];
            for (int i = 0; i < len; i++) {
                buffer[i] = (byte) Integer.parseInt(str.substring(i * 2, i * 2 + 2), 16);
            }
            return buffer;
        }
    }

    public static String prettyJson(String jsonString) {
        JsonElement jsonElement = new JsonParser().parse(jsonString);
        return new GsonBuilder().setPrettyPrinting().create().toJson(jsonElement);
    }

    public static String readStream(Reader reader) throws IOException {
        BufferedReader in = new BufferedReader(reader);
        StringBuilder out = new StringBuilder();
        char data[] = new char[1024];
        int bytesRead;
        while ((bytesRead = in.read(data, 0, 1024)) != -1) {
            out.append(new String(data, 0, bytesRead));
        }
        in.close();

        return out.toString();
    }

    public static String readStream(InputStream stream) throws IOException {
        byte[] buffer = new byte[8192];
        int bytesRead;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        while ((bytesRead = stream.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
        stream.close();

        return new String(Base64.getEncoder().encode(output.toByteArray()));
    }

    public static String randomString(int len){
        final String alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder(len);
        for(int i = 0; i < len; i++) {
            sb.append(alphabet.charAt(rnd.nextInt(alphabet.length())));
        }
        return sb.toString();
    }

    public static boolean isValidURL(String urlStr) {
        try {
            new URL(urlStr);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    public static <T> T nvl(T val, T ifNull) {
        if (val == null) {
            return ifNull;
        }
        return val;
    }
}
