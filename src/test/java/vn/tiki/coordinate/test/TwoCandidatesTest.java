package vn.tiki.coordinate.test;

import io.gridgo.utils.ThreadUtils;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.ZooKeeper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import vn.tiki.coordinate.LeaderElection;
import vn.tiki.coordinate.LeadershipListener;
import vn.tiki.coordinate.Member;
import vn.tiki.coordinate.zookeeper.ZooKeeperLeaderElection;
import vn.tiki.coordinate.zookeeper.support.ZooKeeperHelper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static vn.tiki.coordinate.test.TestZooKeeperLeaderElection.DEFAULT_ZOOKEEPER;

@Slf4j
public class TwoCandidatesTest {
    private ZooKeeper zooKeeper;

    @Before
    public void initZooKeeper() {
        zooKeeper = ZooKeeperHelper.initZooKeeper(DEFAULT_ZOOKEEPER, 10000, 30000);
    }

    @After
    public void tearDown() throws InterruptedException {
        if (zooKeeper != null) {
            zooKeeper.close();
        }
    }

    @Test
    public void test2Members_ToGetSingleLeader() throws Exception {
        log.info("test 2 candidates");
        final String rootPath = "/test-2-candidates";

        final CountDownLatch startSignal = new CountDownLatch(1);
        final CountDownLatch doneSignal = new CountDownLatch(2);

        final AtomicReference<String> receivedLeader1 = new AtomicReference<>();
        Thread thread1 = createLeaderElectionThread(rootPath, "member1", startSignal, event -> {
            receivedLeader1.set(event.getLeader().getId());
            doneSignal.countDown();
        });

        final AtomicReference<String> receivedLeader2 = new AtomicReference<>();
        Thread thread2 = createLeaderElectionThread(rootPath, "member2", startSignal, event -> {
            receivedLeader2.set(event.getLeader().getId());
            doneSignal.countDown();
        });

        try {
            startSignal.countDown();
            doneSignal.await(15, TimeUnit.SECONDS);
            assertEquals(0, doneSignal.getCount());
            assertEquals(receivedLeader1.get(), receivedLeader2.get());
        } finally {
            thread1.interrupt();
            thread2.interrupt();
        }
        ThreadUtils.sleep(200);
    }

    private Thread createLeaderElectionThread(@NonNull String rootPath, @NonNull String memberName,
                                              @NonNull CountDownLatch startSignal, @NonNull LeadershipListener onLeadershipEventListener) {
        final Thread thread = new Thread(() -> {
            LeaderElection leaderElection = new ZooKeeperLeaderElection(zooKeeper, rootPath);
            leaderElection.addLeadershipListener(onLeadershipEventListener);

            try {
                startSignal.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            leaderElection.nominateCandidate(Member.newBuilder() //
                    .id(memberName)//
                    .build());

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    // do nothing...
                }
            }
        });
        thread.start();
        return thread;
    }


}
