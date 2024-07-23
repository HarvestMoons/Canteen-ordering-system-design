package customExceptions;

public class IllegalOrderStateChangeException extends Exception{
    public IllegalOrderStateChangeException(int oldState,int newState){
        super("非法的订单状态变化："+oldState+"->"+newState);
    }
}
