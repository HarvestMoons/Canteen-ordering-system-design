package customExceptions;

public class OrderStateUndefinedExpection extends Exception{
    //构造函数
    public OrderStateUndefinedExpection(int undefinedState){
        super("未定义的订单状态："+undefinedState);
    }
}
