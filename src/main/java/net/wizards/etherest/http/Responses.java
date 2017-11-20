package net.wizards.etherest.http;

public class Responses {
    public static Response emptyOk() {
        return new Response(null, null, Status.OK, null);
    }

    /*public static Response emptyOk() {
        return emptyOk(null);
    }*/

    public static Response jsonOk(String body) {
        return new Response(null, "application/json", Status.OK, body);
    }

    /*public static Response jsonOk(String body) {
        return jsonOk(null, body);
    }*/

    public static Response plaintextOk(String body) {
        return new Response(null, "text/plain", Status.OK, body);
    }

    /*public static Response plaintextOk(String body) {
        return plaintextOk(null, body);
    }*/

    public static Response emptyNotFound() {
        return new Response(null, null, Status.NOT_FOUND, null);
    }

    /*public static Response emptyNotFound() {
        return emptyNotFound(null);
    }*/

    public static Response emptyMethodNotAllowed() {
        return new Response(null, null, Status.METHOD_NOT_ALLOWED, null);
    }

    /*public static Response emptyMethodNotAllowed() {
        return emptyMethodNotAllowed(null);
    }*/

    public static Response emptyInternalServerError() {
        return new Response(null, null, Status.INTERNAL_SERVER_ERROR, null);
    }

    /*public static Response emptyInternalServerError() {
        return emptyInternalServerError(null);
    }*/

    public static Response emptyForbidden() {
        return new Response(null, null, Status.FORBIDDEN, null);
    }

    /*public static Response emptyForbidden() {
        return emptyForbidden(null);
    }*/

    public static Response emptyBadRequest() {
        return new Response(null, null, Status.BAD_REQUEST, null);
    }

    /*public static Response emptyBadRequest() {
        return emptyBadRequest(null);
    }*/

    public static Response emptyUnauthorized() {
        return new Response(null, null, Status.UNAUTHORIZED, null);
    }

    /*public static Response emptyUnauthorized() {
        return emptyUnauthorized(null);
    }*/
}
