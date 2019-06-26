package vn.tiki.coordinate;

public interface LeaderElection {

    Member getLeader();

    Member getCandidate();

    void nominateCandidate(Member member);

    void cancelCandidate();

    LeadershipListeningDisposable addLeadershipListener(LeadershipListener listener);
}
