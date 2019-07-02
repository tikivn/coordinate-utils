package vn.tiki.coordinate.zookeeper;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import vn.tiki.coordinate.exception.LeaderElectionException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ZooKeeperLeaderElectionTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test(expected = LeaderElectionException.class)
    public void test_CannotCreateNode()  {
        ZooKeeperLeaderElection leaderElection = spy(new ZooKeeperLeaderElection(null, ""));
        doReturn(null).when(leaderElection).createNode(anyString(), anyBoolean(), anyBoolean(), eq(null));
        leaderElection.init();
    }

    @Test(expected = RuntimeException.class)
    public void test_ConnectionHasInterruptedException() throws KeeperException, InterruptedException {
        ZooKeeper zooKeeper = mock(ZooKeeper.class);
        doThrow(InterruptedException.class).when(zooKeeper).exists(anyString(), anyBoolean());
        new ZooKeeperLeaderElection(zooKeeper, "");
    }

    @Test(expected = RuntimeException.class)
    public void test_ConnectionHasKeeperException() throws KeeperException, InterruptedException {
        ZooKeeper zooKeeper = mock(ZooKeeper.class);
        doThrow(mock(KeeperException.class)).when(zooKeeper).exists(anyString(), anyBoolean());
        new ZooKeeperLeaderElection(zooKeeper, "");
    }

}