package vn.tiki.coordinate.test;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.zookeeper.ZooKeeper;
import org.junit.Test;

import io.gridgo.bean.BObject;
import lombok.extern.slf4j.Slf4j;
import vn.tiki.coordinate.LeaderElection;
import vn.tiki.coordinate.Member;
import vn.tiki.coordinate.zookeeper.ZooKeeperLeaderElection;
import vn.tiki.coordinate.zookeeper.support.ZooKeeperHelper;

@Slf4j
public class TestZooKeeperLeaderElection {

    @Test
    public void testSingle() throws Exception {
        ZooKeeper zooKeeper = ZooKeeperHelper.initZooKeeper("localhost:2181", 1000, 3000);
        LeaderElection leaderElection = new ZooKeeperLeaderElection(zooKeeper, "/test-leader-election");

        final AtomicReference<BObject> ref = new AtomicReference<>();

        final CountDownLatch doneSignal = new CountDownLatch(1);
        leaderElection.addLeadershipEvent(event -> {
            ref.set(event.getLeader().getData());
            doneSignal.countDown();
        });

        BObject candidateData = BObject.ofSequence("key", "value1");

        log.info("nominate a candidate...");
        leaderElection.nominateCandidate(Member.newBuilder() //
                .id("member1")//
                .data(candidateData) //
                .build());

        doneSignal.await(5, TimeUnit.SECONDS);

        assertEquals(candidateData, ref.get());
    }
}
