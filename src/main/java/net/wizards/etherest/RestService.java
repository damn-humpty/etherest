package net.wizards.etherest;

import net.wizards.etherest.annotation.RequestMapping;
import net.wizards.etherest.bot.EtherBot;
import net.wizards.etherest.http.Request;
import net.wizards.etherest.http.Response;
import net.wizards.etherest.http.Responses;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.IOException;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

class RestService extends AbstractService {
    private Set<String> localIpAddresses;
    private ScheduledExecutorService scheduler;

    private static RestService instance;
    private static final Marker TAG_EXEC = MarkerManager.getMarker("EXEC");

    private RestService() throws IOException {
        reconfig();
        this.serverSocket = new ServerSocket(port);
        service = Executors.newFixedThreadPool(poolSize);
        scheduler = Executors.newScheduledThreadPool(5);
    }

    static synchronized RestService getInstance() throws IOException {
        if (instance == null) {
            instance = new RestService();
        }
        return instance;
    }

    @Override
    public void reconfigDependencies() {
        try {
            localIpAddresses = new HashSet<>();
            Enumeration<NetworkInterface> networkInterfaces =  NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                localIpAddresses.addAll(
                        networkInterfaces.nextElement()
                                .getInterfaceAddresses()
                                .stream()
                                .map(a -> a.getAddress().getHostAddress())
                                .collect(Collectors.toList()));
            }
            localIpAddresses.addAll(Arrays.stream(config.getTrustedIp().split(",\\s*")).collect(Collectors.toSet()));
        } catch (SocketException e) {
            logger.error(TAG_CLASS, "Failed to enumerate local network interfaces. REST service control disabled.", e);
        }
    }

    @Override
    public int getPort() {
        return config.getPortRestListener();
    }

    @Override
    void shutdown() {
        scheduler.shutdownNow();
        super.shutdown();
    }

    @Override
    public String getDispatcherThreadName() {
        return "rest_lsnr[disp]";
    }

    @Override
    public int getPoolSize() {
        return config.getRestListenerPoolSize();
    }

    private boolean addressAllowed(String inetAddress) {
        return localIpAddresses.contains(inetAddress);
    }

    @SuppressWarnings("unused")
    @RequestMapping(value="/rest/shutdown")
    public Response shutdown(Request request) {
        logger.info(TAG_EXEC, request.getResource());
        if (addressAllowed(request.getRemoteAddr())) {
            this.shutdown();
            EtherBot.get().shutdown();
            App.getApp().removePidFile();
            return Responses.emptyOk();
        } else {
            return Responses.emptyForbidden();
        }
    }

    @SuppressWarnings("unused")
    @RequestMapping(value="/rest/self_test")
    public Response selfTest(Request request) {
        logger.info(TAG_EXEC, request.getResource());
        if (addressAllowed(request.getRemoteAddr())) {
            return Responses.plaintextOk("OK");
        } else {
            return Responses.emptyForbidden();
        }
    }

    @SuppressWarnings("unused")
    @RequestMapping(value="/rest/stats")
    public Response stats(Request request) {
        logger.info(TAG_EXEC, request.getResource());
        if (addressAllowed(request.getRemoteAddr())) {
            return Responses.jsonOk(getStats());
        } else {
            return Responses.emptyForbidden();
        }
    }

    @SuppressWarnings("unused")
    @RequestMapping(value="/rest/version")
    public Response version(Request request) {
        logger.info(TAG_EXEC, request.getResource());
        if (addressAllowed(request.getRemoteAddr())) {
            String versionInfo = config.getBuildNumber();
            return Responses.plaintextOk(versionInfo == null ? "unspecified" : versionInfo);
        } else {
            return Responses.emptyForbidden();
        }
    }

/*
    @SuppressWarnings("unused")
    @RequestMapping(value="/rest/auth2", params = {"did", "key"})
    public Response auth2(Request request) {
        logger.info(TAG_EXEC, request.getResource());
        try {
            String token = Auth.buildToken(
                    request.getQueryParam("did"),
                    request.getQueryParam("key"),
                    request.getRemoteAddr());
            return Responses.jsonOk(String.format("{\"token\":\"%s\"}", token));
        } catch (Exception e) {
            return Responses.emptyForbidden();
        }
    }
*/

/*
    @SuppressWarnings("unused")
    @AuthRequired
    @RequestMapping(value="/rest/register", params = {"did"}, method = {Request.Method.POST})
    public Response login(Request request) {
        logger.info(TAG_EXEC, request.getResource());
        JsonElement responseJson = new JsonParser()
                .parse(db.read("begin ? := gtr_utils.register(?); end;", request.getQueryParams(), false));

        return Responses.jsonOk(new Gson().toJson(responseJson.getAsJsonObject()));
    }
*/


    /*@SuppressWarnings("unused")
    @AuthRequired
    @RequestMapping(value="/rest/send_gps_location", params = {"did", "lat", "lon", "when"}, method = {Request.Method.POST})
    public Response gpsLocation(Request request) {
        logger.info(TAG_EXEC, request.getResource());

        return Responses.emptyOk();
    }*/

/*
    @SuppressWarnings("unused")
    @RequestMapping(value="/rest/reconfig")
    public Response reconfig(Request request) {
        logger.info(TAG_EXEC, request.getResource());
        if (addressAllowed(request.getRemoteAddr())) {
            config.setConfig(request.getQueryParam("config", EtherBot.getConfigFile()));
            this.reconfig();
            return Responses.emptyOk();
        } else {
            return Responses.emptyForbidden();
        }
    }
*/

    /*@SuppressWarnings("unused")
    @AuthRequired
    @RequestMapping(value="/rest/login2", params = {"did", "login", "pwd", "when"}, method = {Request.Method.POST})
    public Response login2(Request request) {
        logger.info(TAG_EXEC, request.getResource());
        JsonElement responseJson = new JsonParser()
                .parse(db.read("begin ? := gtr_utils.login(?); end;", request.getQueryParams(), false));

        return Responses.jsonOk(new Gson().toJson(responseJson.getAsJsonObject()));
    }*/

    /*@SuppressWarnings("unused")
    @AuthRequired
    @RequestMapping(value="/rest/logout", params = {"when", "log_shf_id"}, method = {Request.Method.POST})
    public Response logout(Request request) {
        logger.info(TAG_EXEC, request.getResource());
        db.write("begin ? := gtr_utils.logout(?); end;", request.getQueryParams());

        return Responses.emptyOk();
    }*/

    /*@SuppressWarnings("unused")
    @AuthRequired
    @RequestMapping(value="/rest/get_photo", params = {"pho_id"}, method = {Request.Method.POST})
    public Response getPhoto(Request request) {
        logger.info(TAG_EXEC, request.getResource());
        String response = db.read("begin ? := gtr_utils.get_photo(?); end;", request.getQueryParams(), false);
        JsonObject responseLoad = new JsonParser().parse(response).getAsJsonObject();

        return Responses.jsonOk(new Gson().toJson(responseLoad));
    }*/

    /*@SuppressWarnings("unused")
    @AuthRequired
    @RequestMapping(value="/rest/get_shift", params = {"emp_id", "millis"}, method = {Request.Method.POST})
    public Response getShift(Request request) {
        logger.info(TAG_EXEC, request.getResource());
        String response = db.read("begin ? := gtr_utils.get_shift(?); end;", request.getQueryParams(), false);
        JsonObject responseLoad = new JsonParser().parse(response).getAsJsonObject();

        return Responses.jsonOk(new Gson().toJson(responseLoad));
    }*/

    /*@SuppressWarnings("unused")
    @AuthRequired
    @RequestMapping(value="/rest/set_notification", params = {"text", "rut_id"}, method = {Request.Method.POST})
    public Response reportOnChannel(Request request) {
        logger.info(TAG_EXEC, request.getResource());
        Notification.notify(request.getQueryParam("text"));

        return Responses.emptyOk();
    }*/

    /*@SuppressWarnings("unused")
    @AuthRequired
    @RequestMapping(value="/rest/set_timeout", params = {"timeout_delay", "rut_id", "nod_num"}, method = {Request.Method.POST})
    public Response setTimeout(Request request) {
        logger.info(TAG_EXEC, request.getResource());

        long timeout = Long.parseLong(request.getQueryParam("timeout_delay"))
                - Long.parseLong(request.getQueryParam("delay"));
        if (timeout > 0) {
            int nodeNum = Integer.parseInt(request.getQueryParam("nod_num"));
            Cache.ntf().merge(
                    request.getQueryParam("rut_id"),
                    new DelayedNotification(
                            scheduler,
                            timeout,
                            request.getQueryParam("text"),
                            null),
                    (o, n) -> { o.cancel(); return n; }
            );
        }

        return Responses.emptyOk();
    }*/

/*
    @SuppressWarnings("unused")
    @AuthRequired
    @RequestMapping(value="/rest/is_known", params = {"did"}, method = {Request.Method.GET})
    public Response isKnown(Request request) {
        logger.info(TAG_EXEC, request.getResource());

        return Responses.jsonOk(db.read("begin ? := gtr_utils.is_known(?); end;", request.getQueryParams(), false));
    }
*/

    /*@SuppressWarnings("unused")
    @AuthRequired
    @RequestMapping(value="/rest/finish_register", params = {"did", "when"}, method = {Request.Method.POST})
    public Response finishRegister(Request request) {
        logger.info(TAG_EXEC, request.getResource());

        return Responses.jsonOk(db.read("begin ? := gtr_utils.finish_register(?); end;", request.getQueryParams(), false));
    }*/

    /*@SuppressWarnings("unused")
    @AuthRequired
    @RequestMapping(value="/rest/init_register_device", params = {"did", "when", "emp_id"}, method = {Request.Method.POST})
    public Response initRegisterDevice(Request request) {
        logger.info(TAG_EXEC, request.getResource());
        String result = db.write("begin ? := gtr_utils.init_register_device(?); end;", request.getQueryParams());

        return Responses.jsonOk(String.format("{\"pin\":\"%s\"}", result));
    }*/

/*
    @SuppressWarnings("unused")
    @AuthRequired
    @RequestMapping(value="/rest/get_objects", params = {"did"}, method = {Request.Method.GET})
    public Response getObjects(Request request) {
        logger.info(TAG_EXEC, request.getResource());
        return Responses.jsonOk(db.read("begin ? := gtr_utils.get_object_list(?); end;", request.getQueryParams(), true));
    }
*/

/*    @SuppressWarnings("unused")
    @AuthRequired
    @RequestMapping(value="/rest/get_routes", params = {"did"}, method = {Request.Method.GET})
    public Response getRoutes(Request request) {
        logger.info(TAG_EXEC, request.getResource());
        return Responses.jsonOk(db.read("begin ? := gtr_utils.get_route_list(?); end;", request.getQueryParams(), true));
    }*/

/*    @SuppressWarnings("unused")
    @AuthRequired
    @RequestMapping(value="/rest/get_nodes", params = {"did"}, method = {Request.Method.GET})
    public Response getNodes(Request request) {
        logger.info(TAG_EXEC, request.getResource());
        return Responses.jsonOk(db.read("begin ? := gtr_utils.get_node_list(?); end;", request.getQueryParams(), true));
    }*/

/*    @SuppressWarnings("unused")
    @AuthRequired
    @RequestMapping(value="/rest/get_tags", params = {"did"}, method = {Request.Method.GET})
    public Response getTags(Request request) {
        logger.info(TAG_EXEC, request.getResource());
        return Responses.jsonOk(db.read("begin ? := gtr_utils.get_tag_list(?); end;", request.getQueryParams(), true));
    }*/

/*    @SuppressWarnings("unused")
    @AuthRequired
    @RequestMapping(value="/rest/get_points", params = {"did"}, method = {Request.Method.GET})
    public Response getPoints(Request request) {
        logger.info(TAG_EXEC, request.getResource());
        return Responses.jsonOk(db.read("begin ? := gtr_utils.get_point_list(?); end;", request.getQueryParams(), true));
    }*/

/*    @SuppressWarnings("unused")
    @AuthRequired
    @RequestMapping(value="/rest/get_subscriptions", params = {"did"}, method = {Request.Method.GET})
    public Response getSubscriptions(Request request) {
        logger.info(TAG_EXEC, request.getResource());
        return Responses.jsonOk(db.read("begin ? := gtr_utils.get_subscription_list(?); end;", request.getQueryParams(), true));
    }*/

/*    @SuppressWarnings("unused")
    @AuthRequired
    @RequestMapping(value="/rest/get_devices", params = {"did"}, method = {Request.Method.GET})
    public Response getDevices(Request request) {
        logger.info(TAG_EXEC, request.getResource());
        return Responses.jsonOk(db.read("begin ? := gtr_utils.get_device_list(?); end;", request.getQueryParams(), true));
    }*/

/*    @SuppressWarnings("unused")
    @AuthRequired
    @RequestMapping(value="/rest/update_device", params = {"did"}, method = {Request.Method.POST})
    public Response updateDevice(Request request) {
        logger.info(TAG_EXEC, request.getResource());
        String result = db.write("begin ? := gtr_utils.update_device(?); end;", request.getQueryParams());
        return Responses.plaintextOk(result);
    }*/

/*    @SuppressWarnings("unused")
    @AuthRequired
    @RequestMapping(value="/rest/delete_device", params = {"did", "dev_id"}, method = {Request.Method.DELETE})
    public Response deleteDevice(Request request) {
        logger.info(TAG_EXEC, request.getResource());
        String result = db.write("begin ? := gtr_utils.delete_device(?); end;", request.getQueryParams());
        return Responses.plaintextOk(result);
    }*/

/*    @SuppressWarnings("unused")
    @AuthRequired
    @RequestMapping(value="/rest/update_object", params = {"did"}, method = {Request.Method.POST})
    public Response updateObject(Request request) {
        logger.info(TAG_EXEC, request.getResource());
        String result = db.write("begin ? := gtr_utils.update_object(?); end;", request.getQueryParams());
        return Responses.plaintextOk(result);
    }*/

/*    @SuppressWarnings("unused")
    @AuthRequired
    @RequestMapping(value="/rest/delete_object", params = {"did", "obj_id"}, method = {Request.Method.DELETE})
    public Response deleteObject(Request request) {
        logger.info(TAG_EXEC, request.getResource());
        String result = db.write("begin ? := gtr_utils.delete_object(?); end;", request.getQueryParams());
        return Responses.plaintextOk(result);
    }*/

/*
    @SuppressWarnings("unused")
    @AuthRequired
    @RequestMapping(value="/rest/update_route", params = {"did"}, method = {Request.Method.POST})
    public Response updateRoute(Request request) {
        logger.info(TAG_EXEC, request.getResource());
        String result = db.write("begin ? := gtr_utils.update_route(?); end;", request.getQueryParams());
        return Responses.plaintextOk(result);
    }

    @SuppressWarnings("unused")
    @AuthRequired
    @RequestMapping(value="/rest/delete_route", params = {"did", "rut_id"}, method = {Request.Method.DELETE})
    public Response deleteRoute(Request request) {
        logger.info(TAG_EXEC, request.getResource());
        String result = db.write("begin ? := gtr_utils.delete_route(?); end;", request.getQueryParams());
        return Responses.plaintextOk(result);
    }

    @SuppressWarnings("unused")
    @AuthRequired
    @RequestMapping(value="/rest/update_node", params = {"did"}, method = {Request.Method.POST})
    public Response updateNode(Request request) {
        logger.info(TAG_EXEC, request.getResource());
        String result = db.write("begin ? := gtr_utils.update_node(?); end;", request.getQueryParams());
        return Responses.plaintextOk(result);
    }

    @SuppressWarnings("unused")
    @AuthRequired
    @RequestMapping(value="/rest/delete_node", params = {"did", "nod_id"}, method = {Request.Method.DELETE})
    public Response deleteNode(Request request) {
        logger.info(TAG_EXEC, request.getResource());
        String result = db.write("begin ? := gtr_utils.delete_node(?); end;", request.getQueryParams());
        return Responses.plaintextOk(result);
    }

    @SuppressWarnings("unused")
    @AuthRequired
    @RequestMapping(value="/rest/update_point", params = {"did"}, method = {Request.Method.POST})
    public Response updatePoint(Request request) {
        logger.info(TAG_EXEC, request.getResource());
        String result = db.write("begin ? := gtr_utils.update_point(?); end;", request.getQueryParams());
        return Responses.plaintextOk(result);
    }

    @SuppressWarnings("unused")
    @AuthRequired
    @RequestMapping(value="/rest/delete_point", params = {"did", "pnt_id"}, method = {Request.Method.DELETE})
    public Response deletePoint(Request request) {
        logger.info(TAG_EXEC, request.getResource());
        String result = db.write("begin ? := gtr_utils.delete_point(?); end;", request.getQueryParams());
        return Responses.plaintextOk(result);
    }

    @SuppressWarnings("unused")
    @AuthRequired
    @RequestMapping(value="/rest/update_subscription", params = {"did"}, method = {Request.Method.POST})
    public Response updateSubscription(Request request) {
        logger.info(TAG_EXEC, request.getResource());
        String result = db.write("begin ? := gtr_utils.update_subscription(?); end;", request.getQueryParams());
        return Responses.plaintextOk(result);
    }

    @SuppressWarnings("unused")
    @AuthRequired
    @RequestMapping(value="/rest/delete_subscription", params = {"did", "sub_id"}, method = {Request.Method.DELETE})
    public Response deleteSubscription(Request request) {
        logger.info(TAG_EXEC, request.getResource());
        String result = db.write("begin ? := gtr_utils.delete_subscription(?); end;", request.getQueryParams());
        return Responses.plaintextOk(result);
    }

    @SuppressWarnings("unused")
    @AuthRequired
    @RequestMapping(value="/rest/update_tag", params = {"did"}, method = {Request.Method.POST})
    public Response updateTag(Request request) {
        logger.info(TAG_EXEC, request.getResource());
        String result = db.write("begin ? := gtr_utils.update_tag(?); end;", request.getQueryParams());
        return Responses.plaintextOk(result);
    }

    @SuppressWarnings("unused")
    @AuthRequired
    @RequestMapping(value="/rest/delete_tag", params = {"did", "tag_id"}, method = {Request.Method.DELETE})
    public Response deleteTag(Request request) {
        logger.info(TAG_EXEC, request.getResource());
        String result = db.write("begin ? := gtr_utils.delete_tag(?); end;", request.getQueryParams());
        return Responses.plaintextOk(result);
    }
*/

    /*@SuppressWarnings("unused")
    @RequestMapping(value="/rest/get_log", method = {Request.Method.GET})
    public Response getLog(Request request) {
        logger.info(TAG_EXEC, request.getResource());
        if (addressAllowed(request.getRemoteAddr())) {
            return Responses.jsonOk(db.read("begin ? := gtr_utils.get_log(?); end;", request.getQueryParams(), true));
        } else {
            return Responses.emptyForbidden();
        }
    }

    @SuppressWarnings("unused")
    @RequestMapping(value="/rest/get_event", method = {Request.Method.GET})
    public Response getEvent(Request request) {
        logger.info(TAG_EXEC, request.getResource());
        if (addressAllowed(request.getRemoteAddr())) {
            return Responses.jsonOk(db.read("begin ? := gtr_utils.get_events(?); end;", request.getQueryParams(), true));
        } else {
            return Responses.emptyForbidden();
        }
    }*/

/*
    @SuppressWarnings("unused")
    @AuthRequired
    @RequestMapping(value="/rest/get_route", params = {"obj_id"}, method = {Request.Method.POST})
    public Response getRoute(Request request) {
        logger.info(TAG_EXEC, request.getResource());
        String response = db.read("begin ? := gtr_utils.get_route(?); end;", request.getQueryParams(), true);

        return Responses.jsonOk(response);
    }

    @SuppressWarnings("unused")
    @AuthRequired
    @RequestMapping(value="/rest/node_checkout", params = {"did", "when", "emp_id", "pnt_id", "sid"}, method = Request.Method.POST)
    public Response nodeCheckout(Request request) {
        logger.info(TAG_EXEC, request.getResource());

        db.write("begin ? := gtr_utils.node_checkout(?); end;", request.getQueryParams());

        return Responses.emptyOk();
    }

    @SuppressWarnings("unused")
    @AuthRequired
    @RequestMapping(value="/rest/monitor", params = {"did"}, method = Request.Method.POST)
    public Response deviceMonitor(Request request) {
        logger.info(TAG_EXEC, request.getResource());

        return Responses.emptyOk();
    }

    @SuppressWarnings("unused")
    @RequestMapping(value="/rest/get_event", method = {Request.Method.GET})
    public Response getEvent(Request request) {
        logger.info(TAG_EXEC, request.getResource());
        return Responses.plaintextOk(db.readCSV("begin ? := gtr_evt_utils.get_event_list(?); end;", request.getQueryParams()));
    }
*/
}

