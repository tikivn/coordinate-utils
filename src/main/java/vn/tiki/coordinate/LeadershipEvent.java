package vn.tiki.coordinate;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LeadershipEvent {

    private LeadershipEventType type;

    private Member oldLeader;

    private Member leader;
}
