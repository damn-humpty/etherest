package net.wizards.etherest;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import net.wizards.etherest.annotation.RequestMapping;
import net.wizards.etherest.database.Stats;
import net.wizards.etherest.http.Request;
import net.wizards.etherest.http.Response;
import net.wizards.etherest.http.Responses;
import net.wizards.etherest.util.Misc;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

abstract class AbstractService implements Runnable {
    int port = -1;
    int poolSize = -1;
    ServerSocket serverSocket;
    ExecutorService service;
    Config config;
    private Stats stats;
    private static Map<MappingKey, MappingValue> workers = new HashMap<>();
    private volatile boolean shuttingDown;

    final Logger logger = LogManager.getLogger();
    final Marker TAG_CLASS = MarkerManager.getMarker(getClass().getSimpleName());
    private static final Marker TAG_REST = MarkerManager.getMarker("REST_TALK");

    private void initWorkerMappings() {
        for (final java.lang.reflect.Method method : getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(RequestMapping.class)) {
                RequestMapping map = method.getAnnotation(RequestMapping.class);
                for (String resource : map.value()) {
                    for (Request.Method httpMethod : map.method()) {
                        workers.put(
                                new MappingKey(resource, httpMethod),
                                new MappingValue(method, map.params(), map.produces()));
                    }
                }
            }
        }
    }

    @Override
    public void run() {
        Thread.currentThread().setName(getDispatcherThreadName());
        logger.info(TAG_CLASS, "Listener started on port " + serverSocket.getLocalPort());
        while (!service.isShutdown()) {
            try {
                final Socket clientSocket = serverSocket.accept();
                service.submit(() -> {
                    Thread.currentThread().setName(String.format("rest_worker[%s]", Thread.currentThread().getId()));
                    Response response = null;
                    try {
                        Request request = Request.from(clientSocket);
                        if (config.isLogRestConnectedClient()) {
                            logger.debug(TAG_CLASS, String.format("Connected client [%s]", request.getRemoteAddr()));
                        }
                        if (config.isLogRestRequests()) {
                            logger.debug(TAG_REST, Misc.prettyJson(request.getRestParams()));
                        }
                        try {
                            MappingValue mappingValue = workers.get(new MappingKey(request.getResource(), request.getMethod()));
                            if (request.hasAllQueryParams(mappingValue.requiredParams)) {
                                response = ((Response) mappingValue.method.invoke(this, request)).withSocket(clientSocket);
                                response.setContentType(mappingValue.contentType);
                            } else {
                                response = Responses.emptyBadRequest().withSocket(clientSocket);
                            }
                        } catch (InvocationTargetException e) {
                            logger.error(TAG_CLASS, "Internal exception", e);
                            response = Responses.emptyInternalServerError().withSocket(clientSocket);
                        } catch (Exception e) {
                            logger.error(TAG_CLASS, "Reflexive call failed", e);
                            response = Responses.emptyMethodNotAllowed().withSocket(clientSocket);
                        }
                    } catch (Exception e) {
                        logger.error(TAG_CLASS, "Failed to process incoming request", e);
                        response = Responses.emptyInternalServerError().withSocket(clientSocket);
                    } finally {
                        try {
                            if (response != null) {
                                response.send();
                            }
                            clientSocket.close();
                        } catch (IOException e) {
                            logger.error(TAG_CLASS, "Failed to close client socket", e);
                        }
                    }
                });
            } catch (IOException e) {
                if (shuttingDown) {
                    logger.info(TAG_CLASS, "Listener socket exception, shutdown in progress");
                } else {
                    logger.error(TAG_CLASS, "Listener socket exception, perhaps shutdown was requested", e);
                }
                service.shutdown();
            }
        }
        logger.info(TAG_CLASS, "Listener stopped");
    }

    void reconfig() {
        initWorkerMappings();
        config = Config.get();
        stats = Stats.getInstance();
        int portTmp = getPort();
        int poolSizeTmp = getPoolSize();

        if (port == -1 || poolSize == -1) {
            port = portTmp;
            poolSize = poolSizeTmp;
        } else {
            if (portTmp != port) {
                logger.error(TAG_CLASS, "Can't reconfigure listener port dynamically, restart is required");
            }
            if (poolSizeTmp != poolSize && service instanceof ThreadPoolExecutor) {
                poolSize = poolSizeTmp;
                ((ThreadPoolExecutor) service).setCorePoolSize(poolSizeTmp);
                ((ThreadPoolExecutor) service).setMaximumPoolSize(poolSizeTmp);
                logger.info(TAG_CLASS, "Pool size resized to " + poolSize);
            }
        }

        reconfigDependencies();
    }

    void shutdown() {
        shuttingDown = true;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                logger.debug(TAG_CLASS, "Failed to gracefully close socket", e);
            } finally {
                service.shutdown();
            }
        }
    }

    String getStats() {
        String result = null;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            JsonWriter writer = new JsonWriter(new OutputStreamWriter(baos,"UTF-8"));
            writer.setIndent("    ");
            writer.beginObject();
            writer.name("executor").jsonValue(new Gson().toJson(Stats.PoolExecutorStats.of(service)));
            writer.name("memSize").value(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
            writer.name("stats").jsonValue(new Gson().toJson(stats));
            writer.endObject();
            writer.close();
            result = baos.toString("UTF-8");
        } catch (IOException e) {
            logger.error(TAG_CLASS, "Failed to collect service statistics", e);
        }
        return result;
    }

    public abstract void reconfigDependencies();
    public abstract int getPort();
    public abstract int getPoolSize();

    public String getDispatcherThreadName() { return "svc_lsnr[disp]"; }

    private static class MappingKey {
        private String resource;
        private Request.Method method;

        private MappingKey(String resource, Request.Method method) {
            this.resource = resource;
            this.method = method;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MappingKey that = (MappingKey) o;

            return resource.equals(that.resource) && method == that.method;
        }

        @Override
        public int hashCode() {
            int result = resource.hashCode();
            result = 31 * result + method.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "MappingKey{" +"resource='" + resource + '\'' + ", method=" + method + '}';
        }
    }

    private static class MappingValue {
        private Method method;
        private String[] requiredParams;
        private String contentType;

        MappingValue(Method method, String[] requiredParams, String contentType) {
            this.method = method;
            this.requiredParams = requiredParams;
            this.contentType = contentType;
        }
    }
}
