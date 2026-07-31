package zm.organization.common.exception;

public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException() {
        super("Search rate limit exceeded for this caller");
    }
}
