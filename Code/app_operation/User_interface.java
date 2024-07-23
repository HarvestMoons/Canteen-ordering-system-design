package app_operation;

import app_users.*;
import customExceptions.QuitOrderProcessException;
import database_operation.Table_handler;
import utility.Constants;

import java.sql.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class User_interface {

    // 用户登录方法
    public static User userLogin(Scanner scanner, Connection connection) throws SQLException {
        while (!SystemState.isUserLogin) {
            System.out.println("=====请输入账号密码=====");
            System.out.print("账号:\t");
            String account = getValidIdentifier(scanner, Constants.MAX_SHORT_STRING_LENGTH);
            System.out.print("密码:\t");
            String password = getValidIdentifier(scanner, Constants.MAX_SHORT_STRING_LENGTH);
            String querySql = "select type from Users where account = ? and password = ?;";
            PreparedStatement preparedStatement = connection.prepareStatement(querySql);
            preparedStatement.setString(1, account);
            preparedStatement.setString(2, password);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                SystemState.isUserLogin = true;
                int userCode = rs.getInt("type");
                System.out.println("=====" + User_type.getByCode(rs.getInt("type")) + "登录成功" + "=====");
                switch (userCode) {
                    case Constants.ADMIN:
                        return new Admin(connection, scanner, account);
                    case Constants.MERCHANT:
                        return new Merchant(connection, scanner, account);
                    case Constants.CUSTOMER:
                        informCustomerOrderReady(connection, account);
                        return new Customer(connection, scanner, account);
                }
            } else {
                System.out.println("账号或密码错误");
            }
        }
        return null;
    }

    // 用户登出方法
    public static void userLogout() {
        SystemState.isUserLogin = false;
        // Clear the console
        for (int i = 0; i <= 50; i++) {
            System.out.println();
        }

    }

    public static String getValidatedInput(Scanner scanner, ArrayList<String> expectedInputs) {
        String userInput = scanner.nextLine();
        while (!expectedInputs.contains(userInput)) {
            System.out.println("非法输入！请输入以下合法输入中的一项: " + expectedInputs);
            userInput = scanner.nextLine();
        }
        return userInput;
    }

    /** 要求用户输入1-max间的数字，返回该数字 */
    public static int getInt(Scanner scanner, int max) {
        boolean inputSuccess = false;
        Integer result = null;
        while (!inputSuccess) {
            try {
                String userInput = scanner.nextLine();
                result = Integer.parseInt(userInput);
                if (result <= 0 || result > max) {
                    throw new Exception();
                }
                inputSuccess = true;
            } catch (Exception e) {
                System.out.println("非法输入！请输入1~" + max + "间的整数");
            }
        }
        return result;
    }

    // 此方法用于获取用户点餐的菜品序号数列
    public static int[] getValidDishSelection(Scanner scanner, int max)
            throws QuitOrderProcessException {
        System.out.println("请输入菜品序号表示选择，用空格间隔，退出请输入q：");
        int[] selectedDishes = null;
        boolean isValidInput = false;
        while (!isValidInput) {
            String input = scanner.nextLine();
            if ("q".equals(input)) {
                throw new QuitOrderProcessException("您已退出");
            }
            String[] inputArray = input.split(" ");

            boolean isValid = true;
            Set<Integer> dishSet = new HashSet<>();
            for (String s : inputArray) {
                try {
                    int dishNumber = Integer.parseInt(s);
                    if (dishNumber < 1 || dishNumber > max || !dishSet.add(dishNumber)) {
                        isValid = false;
                        break;
                    }
                } catch (NumberFormatException e) {
                    isValid = false;
                    break;
                }
            }
            if (isValid) {
                selectedDishes = dishSet.stream().mapToInt(Integer::intValue).toArray();
                isValidInput = true;
            } else {
                System.out.println("非法输入！请输入1~" + max + "间的互不相同的整数");
            }
        }
        return selectedDishes;
    }

    // 此方法用于获取用户点餐的菜品数量数列
    public static int[] getValidDishSelection(Scanner scanner, int max, int expectedIntNum)
            throws QuitOrderProcessException {
        int[] selectedDishes = null;
        boolean isValidInput = false;
        System.out.println("请输入以上菜品的数量，用空格间隔，退出请输入q：");
        while (!isValidInput) {
            String input = scanner.nextLine();
            if ("q".equals(input)) {
                throw new QuitOrderProcessException("您已退出点餐");
            }
            String[] inputArray = input.split(" ");
            if (inputArray.length != expectedIntNum) {
                System.out.println("请输入与所点菜品一一对应的数量！");
                continue;
            }
            try {
                selectedDishes = Stream.of(inputArray).mapToInt(Integer::parseInt).toArray();
                ;
                for (int dishNumber : selectedDishes) {
                    if (dishNumber < 1 || dishNumber > max) {
                        throw new NumberFormatException();
                    }
                }
                isValidInput = true;
            } catch (NumberFormatException e) {
                System.out.println("非法输入！请输入1~" + max + "间的整数");
            }
        }
        return selectedDishes;
    }

    public static boolean yesOrNo(Scanner scanner, String choiceInfo) {
        System.out.println(choiceInfo);
        String userInput = scanner.nextLine().toUpperCase();
        while (!"Y".equals(userInput) && !"N".equals(userInput)) {
            System.out.println("非法输入！请输入 Y(y) 或 N(n):");
            userInput = scanner.nextLine().toUpperCase();
        }
        return "Y".equals(userInput);
    }

    // 获取商家输入的菜品价格
    public static String getDishPrice(Scanner scanner) {
        String numberStr;
        while (true) {
            System.out.print("菜品价格:\t");
            numberStr = scanner.nextLine();
            // 正则表达式匹配最多两位小数的数字
            String regex = "^-?\\d+(\\.\\d{1,2})?$";
            if (!Pattern.matches(regex, numberStr)) {
                System.out.println("输入无效，请输入一个最多有两位小数的数字");
                continue;
            }
            double number = Double.parseDouble(numberStr);
            if (number < 0) {
                System.out.println("不能输入负数！");
                continue;
            }
            if (number > 9999.99) {
                System.out.println("上限为9999.99,您输入的数值过大！");
                continue;
            }
            break;
        }
        return numberStr;
    }

    // 用户名称输入允许字母、数字、下划线和汉字，防止名称搜索时的SQL注入
    public static String getValidIdentifier(Scanner scanner, int maxLength) {

        String name = scanner.nextLine();
        while (name == null || name.length() > maxLength || !name.matches("[\\w\\u4e00-\\u9fa5]+")) {
            System.out.println("非法输入：此处仅允许输入" + maxLength + "个字符以内的字母、数字和下划线和汉字！");
            name = scanner.nextLine();
        }
        return name;
    }

    public static void showDetailDishMessage(ResultSet allDishesResultSet, Map<Integer, String[]> numToDishName) {
        System.out.println("该菜品的详细信息如下：");
        try {
            for (Map.Entry<Integer, String[]> entry : numToDishName.entrySet()) {
                int index = entry.getKey();
                String[] value = entry.getValue();
                String columnName = value[0];
                String displayName = value[1];
                if ("rating".equals(columnName)) {
                    System.out.println(index + "." + displayName + "：" + allDishesResultSet.getFloat(columnName));
                } else {
                    System.out.println(index + "." + displayName + "：" + allDishesResultSet.getString(columnName));
                }
            }
        } catch (SQLException e) {
            System.out.println("显示菜品详细信息时出错！");
        }
    }

    public static void informCustomerOrderReady(Connection connection, String accountId) {
        try {
            ResultSet resultSet = Table_handler.isItemExists(connection, "orders",
                    accountId, "customer_id", true);
            while (resultSet.next()) {
                int orderState = resultSet.getInt("order_state");
                if (orderState == Constants.FINISHED) {
                    String merchantName = Table_handler.getNameById(connection,
                            "Merchants", "merchant_id", "name",
                            resultSet.getString("merchant_id"));
                    int orderId = resultSet.getInt("order_id");
                    System.out.println("您在" + merchantName + "处的订单（订单号" + orderId + "）已出餐！");
                    Table_handler.orderStateChanged(connection, orderId, Constants.INFORMED);
                }
            }
        } catch (SQLException e) {
            System.err.println("通知用户订单更新时出错");
        }
    }

    public static String getStringWithLengthLimit(Scanner scanner, int maxLength) {
        boolean isInputLegal = false;
        String inputString = "";
        while (!isInputLegal) {
            inputString = scanner.nextLine();
            if (inputString.length() > maxLength) {
                System.out.println("字符串超过长度上限" + maxLength + "，请重新输入：");
                continue;
            }
            isInputLegal = true;
        }
        return inputString;
    }

}
