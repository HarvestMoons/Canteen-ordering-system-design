package customExceptions;

public class UnknownUserTypeException extends Exception{
    public UnknownUserTypeException(int userType){
        super("未知的用户类型！用户代码："+userType);
    }

}
