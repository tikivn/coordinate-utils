package vn.tiki.coordinate;

public interface LeaderElection {

    Member getLeader();

    Member getCandidate();

    void nominateCandidate(Member member);

    LeadershipListeningDisposable addLeadershipEvent(LeadershipListener listener);
}
