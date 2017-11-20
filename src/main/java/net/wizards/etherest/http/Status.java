package net.wizards.etherest.http;

public enum Status {
    OK(200, "OK"),
    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
    INTERNAL_SERVER_ERROR(500, "Internal Server Error");

    private int code;
    private String reason;

    Status(int code, String reason) {
        this.code = code;
        this.reason = reason;
    }

    public String statusLine() { return String.format("HTTP/1.0 %d %s\r\n", code, reason); }
}
