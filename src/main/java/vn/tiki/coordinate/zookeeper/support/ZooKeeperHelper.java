package vn.tiki.coordinate.zookeeper.support;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooKeeper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ZooKeeperHelper {

    private static final List<ZooKeeper> TRACKED_ZOOKEEPER_INSTANCES = new LinkedList<>();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (ZooKeeper zooKeeper : TRACKED_ZOOKEEPER_INSTANCES) {
                try {
                    zooKeeper.close();
                } catch (Exception e) {
                    log.error("error while shutting down zookeeper instance", e);
                }
            }
        }, "zookeeper-instances-shutdown"));
    }

    public static ZooKeeper initZooKeeper(String connnection, int sessionTimeoutMillis, long connectTimeoutMillis) {
        try {
            final CountDownLatch connectedSignal = new CountDownLatch(1);
            final ZooKeeper zooKeeper = new ZooKeeper(connnection, sessionTimeoutMillis, event -> {
                if (KeeperState.SyncConnected.equals(event.getState())) {
                    connectedSignal.countDown();
                }
            });

            if (!connectedSignal.await(connectTimeoutMillis, TimeUnit.MILLISECONDS)) {
                zooKeeper.close();
                throw new TimeoutException("ZooKeeper connecting timeout after " + connectTimeoutMillis + "ms");
            }
            TRACKED_ZOOKEEPER_INSTANCES.add(zooKeeper);
            return zooKeeper;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
