package customExceptions;

public class QuitOrderProcessException extends Exception {
    // 构造函数
    public QuitOrderProcessException(String message) {
        super(message);
    }
}
