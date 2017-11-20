package net.wizards.etherest.http;

import net.wizards.etherest.Config;
import net.wizards.etherest.util.Misc;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class Response {
    private Socket socket;
    private String contentType;
    private Status status;
    private String body;

    private static final Logger logger = LogManager.getLogger();
    private static final Marker TAG_CLASS = MarkerManager.getMarker(Response.class.getSimpleName());
    private static final Marker TAG_HTTP = MarkerManager.getMarker("HTTP_TALK");
    private static final Marker TAG_REST = MarkerManager.getMarker("REST_TALK");

    Response(Socket socket, String contentType, Status status, String body) {
        this.socket = socket;
        this.contentType = contentType;
        this.status = status;
        this.body = body;
    }

    public static Response from(String url) {
        BufferedInputStream in = null;
        StringBuilder out = new StringBuilder();
        try {
            HttpURLConnection connection = (HttpURLConnection) (new URL(url)).openConnection();
            in = new BufferedInputStream(connection.getInputStream());
            byte data[] = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(data, 0, 1024)) != -1) {
                out.append(new String(data, 0, bytesRead, StandardCharsets.UTF_8));
            }
            connection.disconnect();
            in.close();
            return new Response(null, null, Status.OK, out.toString());
        } catch (IOException e) {
            return new Response(null, null, Status.BAD_REQUEST, null);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                logger.error(TAG_CLASS, "Failed to close input stream", e);
            }
        }
    }

    public Response withSocket(Socket socket) {
        this.socket = socket;
        return this;
    }

    public void send() {
        try {
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            String response = status.statusLine();
            int contentLength = 0;
            if (body != null) {
                String contentTypeLine = "Content-RequestType: " + contentType + "; charset=utf-8" + "\r\n";
                contentLength = body.getBytes(StandardCharsets.UTF_8).length;
                String contentLengthLine = "Content-Length: " + contentLength + "\r\n";
                response += contentTypeLine + contentLengthLine + "\r\n" + body;
                if (Config.get().isLogRestResponses() && contentType.equals("application/json")) {
                    if (contentLength > 1024) {
                        logger.debug(TAG_REST, body.substring(0, Math.min(body.length(), 1024)) + "... (output truncated)");
                    } else {
                        logger.debug(TAG_REST, Misc.prettyJson(body));
                    }
                }
            } else {
                response += "\r\n";
            }
            out.write(response);
            out.flush();
            if (Config.get().isLogHttpResponses()) {
                if (contentLength > 1024) {
                    logger.debug(TAG_REST, "Sent HTTP response: " + response.trim().substring(0, 1024) + "... (output truncated)");
                } else {
                    logger.debug(TAG_HTTP, "Sent HTTP response: " + response.trim());
                }
            }
        } catch (IOException e) {
            logger.trace(TAG_CLASS, "Failed to send HTTP response", e);
        }
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getBody() {
        return body;
    }

    public Status getStatus() {
        return status;
    }
}
