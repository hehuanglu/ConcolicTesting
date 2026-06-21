package core.symbolicExecution;


public class UnsatPathException extends RuntimeException {
    public UnsatPathException(String message) {
        super(message);
    }


    public UnsatPathException(String message, Throwable cause) {
        super(message, cause);
    }
}
