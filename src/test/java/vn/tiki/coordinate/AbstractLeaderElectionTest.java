package vn.tiki.coordinate;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import io.gridgo.bean.BObject;
import vn.tiki.coordinate.exception.LeaderElectionException;

public class AbstractLeaderElectionTest {

	@Test(expected = LeaderElectionException.class)
	public void test_nominateCandidate_withException() throws Exception {
		AbstractLeaderElection leaderElection = mock(AbstractLeaderElection.class);
		doThrow(Exception.class).when(leaderElection).tryNominateCandidate(any(Member.class));
		Member member = mock(Member.class);
		leaderElection.nominateCandidate(member);
	}

	@Test
	public void test_fireLeaderChangeDataEvent() {
		AbstractLeaderElection leaderElection = spy(AbstractLeaderElection.class);
		Member oldLeader = Member.newBuilder() //
				.id("")//
				.data(BObject.ofSequence("key", "val1")) //
				.build();
		Member theLeader = Member.newBuilder() //
				.id("")//
				.data(BObject.ofSequence("newkey", "newval")) //
				.build();

		final AtomicReference<Boolean> ref = new AtomicReference<>();
		ref.set(false);

		leaderElection.addLeadershipListener(event -> {
			if (event.getType() == LeadershipEventType.LEADER_DATA_CHANGE)
				ref.set(true);
		});

		leaderElection.leader = oldLeader;
		leaderElection.setLeaderAndTriggerEvent(theLeader);

		assertTrue(ref.get());
	}
}