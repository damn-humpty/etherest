package net.wizards.etherest.http;

import net.wizards.etherest.Config;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.*;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Request {
    private Method method;
    private final String resource;
    private Map<String, String> queryParams;
    private Map<String, String> headerParams;
    private String remoteAddr;
    private Socket socket;
    private String clientId;
    private String phoneId;

    private static final Logger logger = LogManager.getLogger();
    private static final Marker TAG_CLASS = MarkerManager.getMarker(Request.class.getSimpleName());
    private static final Marker TAG_HTTP = MarkerManager.getMarker("HTTP_TALK");
    private static final String NOT_EXISTS = "~not-exists~";

    private Request(Socket socket) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(), Charset.forName("UTF-8")));

            String line = br.readLine();
            if (line == null) throw new RuntimeException("Malformed HTTP header");
            String[] requestParam = line.split(" ");
            try {
                method = Method.valueOf(requestParam[0]);
            } catch (IllegalArgumentException e) {
                method = Method.BAD;
            }
            boolean isPostOrPut = method == Method.POST || method == Method.PUT;
            String[] urlParts = requestParam[1].split("\\?");
            resource = urlParts[0];
            queryParams = urlParts.length > 1 ? getQueryParams(urlParts[1]) : new HashMap<>();

            Pattern headerPattern = Pattern.compile("^([^:]+?):\\s+(.+)$");
            Matcher matcher;
            headerParams = new HashMap<>();
            int contentLength = 0;
            while ((line = br.readLine()) != null && !line.equals("")) {
                matcher = headerPattern.matcher(line);
                if (matcher.matches()) {
                    try {
                        headerParams.put(matcher.group(1), matcher.group(2));
                        if (matcher.group(1).equals("Content-Length")) {
                            contentLength = Integer.parseInt(matcher.group(2));
                        } else if (matcher.group(1).equalsIgnoreCase("did")) {
                            queryParams.put("did", matcher.group(2));
                        }
                    } catch (IllegalStateException | IndexOutOfBoundsException e) {
                        logger.debug(TAG_HTTP, "Malformed HTTP header: " + line);
                    }
                }
            }

            if (isPostOrPut) {
                StringBuilder body = new StringBuilder();
                char[] load = new char[contentLength];
                if (br.read(load, 0, contentLength) != -1) {
                    body.append(load);
                }
                try {
                    JsonReader reader = new JsonReader(new StringReader(body.toString()));
                    reader.setLenient(true);
                    Map<String, String> bodyParams = new Gson().fromJson(reader,
                            new TypeToken<Map<String, String>>(){}.getType());
                    if (bodyParams != null && !bodyParams.isEmpty()) {
                        queryParams.putAll(bodyParams);
                    }
                } catch (Exception e) {
                    logger.error(TAG_CLASS, "Failed to parse body params from this body:" + body.toString(), e);
                    throw new RuntimeException("Invalid body params");
                }
            }

            if (headerParams.containsKey("X-Real-IP")) {
                remoteAddr = headerParams.get("X-Real-IP");
            } else {
                remoteAddr = socket.getInetAddress().getHostAddress();
            }
            this.socket = socket;

            if (Config.get().isLogHttpRequests()) {
                logger.debug(TAG_HTTP, "Accepted HTTP request: " + this);
            }
        } catch (Exception e) {
            logger.error(TAG_CLASS, "Failed to parse HTTP request from socket", e);
            throw new RuntimeException(e);
        }
    }

    public static Request from(Socket socket) {
        return new Request(socket);
    }

    @Override
    public String toString() {
        return String.format("{method=%s, resource=%s, queryParams=%s}, headerParams=%s", method, resource, queryParams, headerParams);
    }

    public String getQueryParam(String paramName) {
        String value = queryParams.get(paramName);
        return value == null ? NOT_EXISTS : value;
    }

    public String getQueryParam(String paramName, String defaultValue) {
        String value = queryParams.get(paramName);
        return value == null ? defaultValue : value;
    }

    public String getHeaderParam(String paramName) {
        String value = headerParams.get(paramName);
        return value == null ? NOT_EXISTS : value;
    }

    private static Map<String, String> getQueryParams(String query) {
        try {
            Map<String, String> params = new HashMap<>();
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                String key = URLDecoder.decode(pair[0], "UTF-8");
                String value = "";
                if (pair.length > 1) {
                    value = URLDecoder.decode(pair[1], "UTF-8");
                }
                params.put(key, value);
            }
            return params;
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }

    public String getRemoteAddr() {
        return remoteAddr;
    }

    public String getResource() {
        return resource;
    }

    public boolean hasAllQueryParams(String[] names) {
        return queryParams.keySet().containsAll(Arrays.asList(names));
    }

    public boolean hasQueryParam(String name) {
        return queryParams.containsKey(name);
    }

    public Socket getSocket() {
        return socket;
    }

    public Method getMethod() {
        return method;
    }

    public String getRestParams() {
        return new Gson().toJson(queryParams);
    }

    public String getClientId() {
        return clientId;
    }

    public void setDeviceId(String deviceId) {
        this.phoneId = deviceId;
        queryParams.put("did", deviceId);
    }

    public JsonObject getQueryParams() {
        return new Gson().toJsonTree(queryParams).getAsJsonObject();
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
        queryParams.put("cid", clientId);
    }

    public enum Method {
        GET, POST, PUT, DELETE, BAD
    }
}
