package customExceptions;

public class OrderNotFoundException extends Exception{
    //构造函数
    public OrderNotFoundException(int orderId){
        super("未找到订单："+orderId);
    }
}