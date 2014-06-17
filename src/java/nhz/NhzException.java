package nhz;

public abstract class NhzException extends Exception {

    protected NhzException() {
        super();
    }

    protected NhzException(String message) {
        super(message);
    }

    protected NhzException(String message, Throwable cause) {
        super(message, cause);
    }

    protected NhzException(Throwable cause) {
        super(cause);
    }

    public static class ValidationException extends NhzException {

        ValidationException(String message) {
            super(message);
        }

        ValidationException(String message, Throwable cause) {
            super(message, cause);
        }

    }

}
