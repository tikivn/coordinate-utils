package vn.tiki.coordinate;

@FunctionalInterface
public interface LeadershipListener {

    void onLeadershipEvent(LeadershipEvent event);
}
