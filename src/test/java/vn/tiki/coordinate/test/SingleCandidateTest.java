package vn.tiki.coordinate.test;


import io.gridgo.bean.BObject;
import io.gridgo.utils.ThreadUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import vn.tiki.coordinate.LeaderElection;
import vn.tiki.coordinate.LeadershipEventType;
import vn.tiki.coordinate.Member;
import vn.tiki.coordinate.zookeeper.ZooKeeperLeaderElection;
import vn.tiki.coordinate.zookeeper.support.ZooKeeperHelper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static vn.tiki.coordinate.test.TestZooKeeperLeaderElection.DEFAULT_ZOOKEEPER;

@Slf4j
public class SingleCandidateTest {

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
    public void test_GetAndVerifyLeaderAndLeaderData() throws Exception {
        log.info("test single candidate for leader, and data of leader");
        LeaderElection leaderElection = new ZooKeeperLeaderElection(zooKeeper, "/test-single");

        final AtomicReference<BObject> ref = new AtomicReference<>();

        final CountDownLatch doneSignal = new CountDownLatch(1);
        leaderElection.addLeadershipListener(event -> {
            ref.set(event.getLeader().getData());
            doneSignal.countDown();
        });

        BObject candidateData = BObject.ofSequence("key", "value1");

        Member candidate = Member.newBuilder() //
                .id("member1")//
                .data(candidateData) //
                .build();
        leaderElection.nominateCandidate(candidate);

        doneSignal.await(5, TimeUnit.SECONDS);
        assertEquals(candidateData, ref.get());
        assertEquals(candidate, leaderElection.getLeader());
    }

    @Test public void test_Update_Data_Of_Leader_From_Current_Leader() throws Exception {
        log.info("test single candidate for leader data changed");
        ZooKeeperLeaderElection leaderElection = new ZooKeeperLeaderElection(zooKeeper, "/test-single");

        final AtomicReference<BObject> ref = new AtomicReference<>();

        final CountDownLatch doneSignal = new CountDownLatch(1);
        final CountDownLatch dataSignal = new CountDownLatch(1);
        leaderElection.addLeadershipListener(event -> {
            if (event.getType() == LeadershipEventType.LEADER_CHANGE) {
                ref.set(event.getLeader().getData());
                doneSignal.countDown();
            } else if (event.getType() == LeadershipEventType.LEADER_DATA_CHANGE) {
                ref.set(event.getLeader().getData());
                dataSignal.countDown();
            }

        });

        BObject candidateData = BObject.ofSequence("key", "value1");

        Member candidate = Member.newBuilder() //
                .id("member1")//
                .data(candidateData) //
                .build();
        leaderElection.nominateCandidate(candidate);
        doneSignal.await(5, TimeUnit.SECONDS);

        System.out.println("leader --> " + leaderElection.getLeader().getId());

        BObject newCandidateData = BObject.ofSequence("newkey", "new1");
        // Change the data of leader node
        changeData(leaderElection, newCandidateData );
        Thread.sleep(300);
        dataSignal.await(5, TimeUnit.SECONDS);
        assertEquals(candidate, leaderElection.getLeader());
        assertNotEquals(newCandidateData, ref.get());
    }

    private void changeData(ZooKeeperLeaderElection leaderElection, BObject newCandidateData) throws KeeperException, InterruptedException {
        String leaderPath = leaderElection.getLeaderPath();
        if (leaderPath == null)
            leaderPath = leaderElection.getPath();

        Member candidate = Member.newBuilder() //
                .id("member1")//
                .data(newCandidateData) //
                .build();
        zooKeeper.setData(leaderPath  , candidate.toBytes(), 0);

        byte[] bytes = this.zooKeeper.getData(leaderPath, true, null);
        Member member = Member.fromBytes(bytes);
        System.out.println("data --> " + member.getData());
    }


}
