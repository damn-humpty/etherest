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
}

