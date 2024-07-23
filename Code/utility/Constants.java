package utility;

public class Constants {
    public static final int ADMIN = 0;
    public static final int MERCHANT = 1;
    public static final int CUSTOMER = 2;

    // 订单状态
    public static final int UNFINISHED = 0;
    public static final int FINISHED = 1;
    public static final int INFORMED = 2;
    public static final int COMMENTED = 3;

    // 性别
    public static final int MALE = 1;
    public static final int FEMALE = 2;
    public static final int OTHERS = 3;

    // 最大点单数量
    public static final int MAX_DISH_ORDER_AMOUNT = 99;
    // showMenu之后的操作类型
    public static final int NO_MORE_OPERATION = 0;
    public static final int ADD_SPECIALTY = 1;
    public static final int MODIFY_DISHES = 2;
    public static final int DELETE_SPECIALTY = 3;

    // 最大评论长度
    public static final int MAX_COMMENT_LENGTH = 256;
    // 最大短字符串长度
    public static final int MAX_SHORT_STRING_LENGTH = 20;
    // 最大指令长度
    public static final int MAX_INSTRUCTION_LENGTH = 5;

    // 一页的最大指令数
    public static final int MAX_DISPLAY_NUM = 10;

}
