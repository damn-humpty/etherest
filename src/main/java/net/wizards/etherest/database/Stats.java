package net.wizards.etherest.database;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

public class Stats {
    private Map<Param, AtomicInteger> stat;

    private static Stats instance;

    private Stats() {
        stat = new ConcurrentHashMap<>();
        for (Param param : Param.values()) {
            stat.put(param, new AtomicInteger(0));
        }
    }

    public AtomicInteger value(Param param) {
        return stat.get(param);
    }

    private enum Param {
        GET_SUBMIT_CNT("Total number of submit requests from Alaris switch"),
        GET_SUBMIT_OK_CNT("Number of successful submit requests from Alaris switch"),
        GET_DLR_CNT("Total number of delivery requests from Alaris switch"),
        GET_DLR_OK_CNT("Number of successful delivery requests from Alaris switch"),
        POST_SUBMIT_CNT("Total number of sendSMS SOAP requests to Parlay X service"),
        POST_SUBMIT_OK_CNT("Number of successful sendSMS SOAP requests to Parlay X service"),
        POST_NOTIF_CNT("Total number of SMS delivery notification SOAP requests from Parlay X service");

        private String descr;

        Param(String descr) {
            this.descr = descr;
        }

        public String getDescr() {
            return descr;
        }
    }

    public synchronized static Stats getInstance() {
        if (instance == null) {
            instance = new Stats();
        }
        return instance;
    }

    public static class PoolExecutorStats {
        // maximum allowed number of threads
        private int largestPoolSize;
        // the maximum allowed number of threads
        private int maximumPoolSize;
        // the approximate number of threads that are actively executing tasks
        private int activeCount;
        // the approximate total number of tasks that have ever been scheduled for execution
        private long taskCount;
        // the approximate total number of tasks that have completed execution
        private long completedTaskCount;
        // the current number of threads in the pool
        private int poolSize;

        private PoolExecutorStats(ExecutorService service) {
            if (!(service instanceof ThreadPoolExecutor)) {
                throw new IllegalArgumentException("Unsupported ExecutorService specified: "
                        + service.getClass().getSimpleName());
            }
            ThreadPoolExecutor executor = (ThreadPoolExecutor) service;
            largestPoolSize = executor.getLargestPoolSize();
            maximumPoolSize = executor.getMaximumPoolSize();
            activeCount = executor.getActiveCount();
            taskCount = executor.getTaskCount();
            completedTaskCount = executor.getCompletedTaskCount();
            poolSize = executor.getPoolSize();
        }

        public static PoolExecutorStats of(ExecutorService service) {
            return new PoolExecutorStats(service);
        }
    }
}
