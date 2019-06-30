package vn.tiki.coordinate;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.cliffc.high_scale_lib.NonBlockingHashMap;

import lombok.Getter;
import lombok.NonNull;
import vn.tiki.coordinate.exception.LeaderElectionException;

public abstract class AbstractLeaderElection implements LeaderElection {

    @Getter
    private Member candidate;

    @Getter
    private Member leader;

    private final AtomicInteger LISTENING_ID_SEED = new AtomicInteger(0);
    private final Map<Integer, LeadershipListener> listeners = new NonBlockingHashMap<>();

    @Override
    public final LeadershipListeningDisposable addLeadershipListener(@NonNull LeadershipListener listener) {
        final int id = LISTENING_ID_SEED.getAndIncrement();
        this.listeners.put(id, listener);
        return () -> {
            this.listeners.remove(id);
        };
    }

    protected void triggerLeadershipEvent(@NonNull LeadershipEvent event) {
        final Iterator<LeadershipListener> it = this.listeners.values().iterator();
        while (it.hasNext()) {
            it.next().onLeadershipEvent(event);
        }
    }

    protected void setLeaderAndTriggerEvent(Member theLeader) {
        Member oldLeader = this.leader;
        this.leader = theLeader;
        if (isLeaderChanged(oldLeader, theLeader)) {
            this.triggerLeadershipEvent(LeadershipEvent.builder() //
                    .type(LeadershipEventType.LEADER_CHANGE) //
                    .oldLeader(oldLeader) //
                    .leader(theLeader) //
                    .build());
        } else if (isLeaderDataChanged(oldLeader, theLeader)) {
            this.triggerLeadershipEvent(LeadershipEvent.builder() //
                    .type(LeadershipEventType.LEADER_DATA_CHANGE) //
                    .leader(theLeader) //
                    .build());
        }
    }

    private boolean isLeaderChanged(Member oldLeader, Member theLeader) {
        return (oldLeader == null && theLeader != null) //
                || oldLeader != null && (theLeader == null || !oldLeader.equals(theLeader));
    }

    private boolean isLeaderDataChanged(Member oldLeader, Member theLeader) {
        return oldLeader != null && oldLeader.equals(theLeader)
                && !oldLeader.getData().equals(theLeader.getData());
    }

    @Override
    public final void nominateCandidate(@NonNull Member member) {
        try {
            if (this.tryNominateCandidate(member)) {
                this.candidate = member;
            }
        } catch (Exception e) {
            throw new LeaderElectionException("An error occurs while nominating member as new candidate", e);
        }
    }

    protected abstract boolean tryNominateCandidate(Member member) throws Exception;
}
