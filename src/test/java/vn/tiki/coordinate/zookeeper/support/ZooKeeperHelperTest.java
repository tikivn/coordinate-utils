package vn.tiki.coordinate.zookeeper.support;

import org.apache.zookeeper.ZooKeeper;
import org.junit.Test;
import vn.tiki.coordinate.test.TestZooKeeperLeaderElection;

import static org.junit.Assert.assertNotNull;

public class ZooKeeperHelperTest {

	@Test
	public void test_initZooKeeper_success() throws InterruptedException {
		String connectString = TestZooKeeperLeaderElection.DEFAULT_ZOOKEEPER;
		int sessionTimeoutMillis = 3000;
		int connectTimeoutMillis = 6000;
		ZooKeeper zooKeeper = ZooKeeperHelper.initZooKeeper(connectString, sessionTimeoutMillis, connectTimeoutMillis);
		assertNotNull(zooKeeper);
		zooKeeper.close();
	}

	@Test(expected = RuntimeException.class)
	public void test_initZooKeeper_error() throws InterruptedException {
		String connectString = "localhost:3191";
		int sessionTimeoutMillis = 2000;
		int connectTimeoutMillis = 3000;
		ZooKeeper zooKeeper = ZooKeeperHelper.initZooKeeper(connectString, sessionTimeoutMillis, connectTimeoutMillis);
		assertNotNull(zooKeeper);
		zooKeeper.close();
	}
}