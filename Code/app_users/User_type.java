package app_users;

import utility.Constants;

public enum User_type {
    ADMINISTRATOR(Constants.ADMIN, "管理员"),
    MERCHANT_USER(Constants.MERCHANT, "食堂商户"),
    ORIGIN_USER(Constants.CUSTOMER, "顾客");

    private final int code;
    private final String name;

    private User_type(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    /** 通过数字获取对应的用户类型 */
    public static String getByCode(int code) {
        for (User_type userType : values()) {
            if (userType.getCode() == code) {
                return userType.getName();
            }
        }
        throw new IllegalArgumentException("Invalid UserType code: " + code);
    }

}
