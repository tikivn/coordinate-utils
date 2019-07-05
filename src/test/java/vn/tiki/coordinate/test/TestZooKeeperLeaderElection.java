package vn.tiki.coordinate.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.zookeeper.ZooKeeper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.gridgo.utils.ThreadUtils;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import vn.tiki.coordinate.LeaderElection;
import vn.tiki.coordinate.LeadershipListener;
import vn.tiki.coordinate.Member;
import vn.tiki.coordinate.zookeeper.ZooKeeperLeaderElection;
import vn.tiki.coordinate.zookeeper.support.ZooKeeperHelper;

@Slf4j
public class TestZooKeeperLeaderElection {

	public static final String DEFAULT_ZOOKEEPER = "localhost:2181";

	private ZooKeeper zooKeeper;

	@Before
	public void initZooKeeper() {
		zooKeeper = ZooKeeperHelper.initZooKeeper(DEFAULT_ZOOKEEPER, 10000, 30000);
	}

	@After
	public void tearDown() throws InterruptedException {
		if (zooKeeper != null)
			zooKeeper.close();
	}

	@Test
	public void testLeaderChange() throws Exception {
		log.info("test 3 candidates");
		final String rootPath = "/test-3-candidates";

		final CountDownLatch startSignal = new CountDownLatch(1);
		final AtomicReference<CountDownLatch> doneSignal = new AtomicReference<>(new CountDownLatch(3));

		final AtomicBoolean keepBusySpin1 = new AtomicBoolean(true);
		final AtomicReference<Thread> threadRef1 = new AtomicReference<>();
		final AtomicReference<String> receivedLeader1 = new AtomicReference<>();
		threadRef1.set(createLeaderElectionThreadAndWaitForInterrupt(rootPath, "member1", startSignal, keepBusySpin1,
				event -> {
					if (keepBusySpin1.get()) {
						String leaderId = event.getLeader().getId();
						receivedLeader1.set(leaderId);
						doneSignal.get().countDown();
					}
				}));

		final AtomicBoolean keepBusySpin2 = new AtomicBoolean(true);
		final AtomicReference<Thread> threadRef2 = new AtomicReference<>();
		final AtomicReference<String> receivedLeader2 = new AtomicReference<>();
		threadRef2.set(createLeaderElectionThreadAndWaitForInterrupt(rootPath, "member2", startSignal, keepBusySpin2,
				event -> {
					if (keepBusySpin2.get()) {
						String leaderId = event.getLeader().getId();
						receivedLeader2.set(leaderId);
						doneSignal.get().countDown();
					}
				}));

		final AtomicBoolean keepBusySpin3 = new AtomicBoolean(true);
		final AtomicReference<Thread> threadRef3 = new AtomicReference<>();
		final AtomicReference<String> receivedLeader3 = new AtomicReference<>();
		threadRef3.set(createLeaderElectionThreadAndWaitForInterrupt(rootPath, "member3", startSignal, keepBusySpin3,
				event -> {
					if (keepBusySpin3.get()) {
						String leaderId = event.getLeader().getId();
						receivedLeader3.set(leaderId);
						doneSignal.get().countDown();
					}
				}));

		try {
			startSignal.countDown();
			doneSignal.get().await(15, TimeUnit.SECONDS);
			assertEquals(0, doneSignal.get().getCount());

			doneSignal.set(new CountDownLatch(2));

			assertEquals(receivedLeader1.get(), receivedLeader2.get());
			assertEquals(receivedLeader1.get(), receivedLeader3.get());

			String currentLeader = receivedLeader1.get();
			switch (currentLeader) {
			case "member1":
				keepBusySpin1.set(false);
				doneSignal.get().await(5, TimeUnit.SECONDS);
				assertEquals(0, doneSignal.get().getCount());
				assertEquals(receivedLeader2.get(), receivedLeader3.get());
				assertNotEquals(currentLeader, receivedLeader2.get());
				break;
			case "member2":
				keepBusySpin2.set(false);
				doneSignal.get().await(5, TimeUnit.SECONDS);
				assertEquals(0, doneSignal.get().getCount());
				assertEquals(receivedLeader1.get(), receivedLeader3.get());
				assertNotEquals(currentLeader, receivedLeader3.get());
				break;
			case "member3":
				keepBusySpin3.set(false);
				doneSignal.get().await(5, TimeUnit.SECONDS);
				assertEquals(0, doneSignal.get().getCount());
				assertEquals(receivedLeader2.get(), receivedLeader1.get());
				assertNotEquals(currentLeader, receivedLeader1.get());
				break;
			}
		} finally {
			keepBusySpin1.set(false);
			keepBusySpin2.set(false);
			keepBusySpin3.set(false);

			ThreadUtils.sleep(100);

			threadRef1.get().interrupt();
			threadRef2.get().interrupt();
			threadRef3.get().interrupt();
		}

		ThreadUtils.sleep(200);
	}

	private Thread createLeaderElectionThreadAndWaitForInterrupt(@NonNull String rootPath, @NonNull String memberName,
			@NonNull CountDownLatch startSignal, @NonNull AtomicBoolean keepBusySpin,
			@NonNull LeadershipListener onLeadershipEventListener) {
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

			while (keepBusySpin.get()) {
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					// do nothing...
				}
			}
			leaderElection.cancelCandidate();

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
