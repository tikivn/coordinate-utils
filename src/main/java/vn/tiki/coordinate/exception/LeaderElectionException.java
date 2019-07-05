package vn.tiki.coordinate.exception;

public class LeaderElectionException extends RuntimeException {

	private static final long serialVersionUID = 2771220224230412843L;

	public LeaderElectionException() {
		super();
	}

	public LeaderElectionException(String message) {
		super(message);
	}

	public LeaderElectionException(String message, Throwable cause) {
		super(message, cause);
	}

	public LeaderElectionException(Throwable cause) {
		super(cause);
	}
}
