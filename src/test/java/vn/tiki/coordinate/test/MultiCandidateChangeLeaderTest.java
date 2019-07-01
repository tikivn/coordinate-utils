package vn.tiki.coordinate.test;

import io.gridgo.bean.BObject;
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

import static org.junit.Assert.assertEquals;
import static vn.tiki.coordinate.test.TestZooKeeperLeaderElection.DEFAULT_ZOOKEEPER;

@Slf4j
public class MultiCandidateChangeLeaderTest {
    private ZooKeeper zooKeeper1;
    private ZooKeeper zooKeeper2;

    @Before
    public void initZooKeeper() {
        zooKeeper1 = ZooKeeperHelper.initZooKeeper(DEFAULT_ZOOKEEPER, 10000, 30000);
        zooKeeper2 = ZooKeeperHelper.initZooKeeper(DEFAULT_ZOOKEEPER, 10000, 30000);
    }

    @After
    public void tearDown() throws InterruptedException {
        if (zooKeeper1 != null) {
            zooKeeper1.close();
        }
        if (zooKeeper2 != null) {
            zooKeeper2.close();
            Thread.sleep(300);
        }
    }

    @Test
    public void test_Leader_Die() throws InterruptedException {
        String rootNodePath  = "/test-multi";
        LeaderElection leaderElection1 = new ZooKeeperLeaderElection(zooKeeper1, rootNodePath);
        final CountDownLatch doneSignal1 = new CountDownLatch(1);
        leaderElection1.addLeadershipListener(event -> {
            //ref.set(event.getLeader().getData());
            System.out.println("Leader for 1: " + event.getLeader().getId());
            doneSignal1.countDown();
        });
        Member member1 = buildMember("member1");
        leaderElection1.nominateCandidate(member1);
        doneSignal1.await(5, TimeUnit.SECONDS);

        LeaderElection leaderElection2= new ZooKeeperLeaderElection(zooKeeper2, rootNodePath);
        final CountDownLatch doneSignal2 = new CountDownLatch(1);
        leaderElection2.addLeadershipListener(event -> {
            System.out.println("Leader for 2: " + event.getLeader().getId());
            assertEquals(member1, event.getOldLeader());
            doneSignal2.countDown();
        });
        Member member2 = buildMember("member2");
        leaderElection2.nominateCandidate(member2);

        assertEquals(member1, leaderElection2.getLeader());
        zooKeeper1.close();
        zooKeeper1 = null;
        doneSignal1.await(5, TimeUnit.SECONDS);
        assertEquals(member2, leaderElection2.getLeader());

    }

    private Member buildMember(String memberName) {
        BObject candidateData = BObject.ofSequence("key", memberName);
        Member candidate = Member.newBuilder() //
                .id(memberName)//
                .data(candidateData) //
                .build();
        return candidate;
    }
}
