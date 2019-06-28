package vn.tiki.coordinate.test;


import io.gridgo.bean.BObject;
import io.gridgo.utils.ThreadUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.ZooKeeper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import vn.tiki.coordinate.LeaderElection;
import vn.tiki.coordinate.Member;
import vn.tiki.coordinate.zookeeper.ZooKeeperLeaderElection;
import vn.tiki.coordinate.zookeeper.support.ZooKeeperHelper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

@Slf4j
public class SingleCandidateTest {
    private static final String DEFAULT_ZOOKEEPER = "localhost:2181";

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
    public void testSingle() throws Exception {
        log.info("test single candidate");
        LeaderElection leaderElection = new ZooKeeperLeaderElection(zooKeeper, "/test-single");

        final AtomicReference<BObject> ref = new AtomicReference<>();

        final CountDownLatch doneSignal = new CountDownLatch(1);
        leaderElection.addLeadershipListener(event -> {
            ref.set(event.getLeader().getData());
            doneSignal.countDown();
        });

        BObject candidateData = BObject.ofSequence("key", "value1");

        leaderElection.nominateCandidate(Member.newBuilder() //
                .id("member1")//
                .data(candidateData) //
                .build());

        doneSignal.await(5, TimeUnit.SECONDS);
        assertEquals(candidateData, ref.get());
        ThreadUtils.sleep(200);
    }
}
