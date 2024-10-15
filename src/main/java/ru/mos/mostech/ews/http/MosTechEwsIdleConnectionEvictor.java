/*
DIT
 */

package ru.mos.mostech.ews.http;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Single thread for all connection managers.
 * close idle connections
 */

@Slf4j
public class MosTechEwsIdleConnectionEvictor {
    static final Logger LOGGER = Logger.getLogger(MosTechEwsIdleConnectionEvictor.class);

    // connection manager set
    private static final HashSet<HttpClientConnectionManager> connectionManagers = new HashSet<>();

    private static final long sleepTimeMs = 1000L * 60;
    private static final long maxIdleTimeMs = 1000L * 60 * 5;

    private static ScheduledExecutorService scheduler = null;

    private static void initEvictorThread() {
        synchronized (connectionManagers) {
            if (scheduler == null) {
                scheduler = Executors.newScheduledThreadPool(1, new ThreadFactory() {
                    int count = 0;
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r, "PoolEvictor-" + count++);
                        thread.setDaemon(true);
                        thread.setUncaughtExceptionHandler((t, e) -> LOGGER.error(e.getMessage(), e));
                        return thread;
                    }
                });
                scheduler.scheduleAtFixedRate(() -> {
                    synchronized (connectionManagers) {
                        // iterate over connection managers
                        for (HttpClientConnectionManager connectionManager : connectionManagers) {
                            connectionManager.closeExpiredConnections();
                            if (maxIdleTimeMs > 0) {
                                connectionManager.closeIdleConnections(maxIdleTimeMs, TimeUnit.MILLISECONDS);
                            }
                        }
                    }
                }, sleepTimeMs, sleepTimeMs, TimeUnit.MILLISECONDS);
            }
        }
    }

    public static void shutdown() throws InterruptedException {
        synchronized (connectionManagers) {
            scheduler.shutdown();
            if (!scheduler.awaitTermination(sleepTimeMs, TimeUnit.MILLISECONDS)) {
                LOGGER.warn("Timed out waiting for tasks to complete");
            }
            scheduler = null;
        }
    }

    /**
     * Add connection manager to evictor thread.
     *
     * @param connectionManager connection manager
     */
    public static void addConnectionManager(HttpClientConnectionManager connectionManager) {
        synchronized (connectionManagers) {
            initEvictorThread();
            connectionManagers.add(connectionManager);
        }
    }

    /**
     * Remove connection manager from evictor thread.
     *
     * @param connectionManager connection manager
     */
    public static void removeConnectionManager(HttpClientConnectionManager connectionManager) {
        synchronized (connectionManagers) {
            connectionManagers.remove(connectionManager);
        }
    }
}
