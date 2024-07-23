package app_users;

import app_operation.SystemState;
import app_operation.User_interface;
import customExceptions.UnknownUserTypeException;
import database_operation.Table_handler;
import utility.Constants;

import java.sql.*;
import java.util.*;

public class Admin extends User {

    public Admin(Connection connection, Scanner scanner, String accountId) {
        super(connection, scanner, accountId);
    }

    @Override
    public void forceInfoSet() {
    }

    @Override
    public void getInstructions() throws SQLException {
        printHelpMessage();
        while (SystemState.isUserLogin) {
            System.out.println("请输入指令 (输入 '-h' 查看所有指令)：");
            String instruction = User_interface.getStringWithLengthLimit(scanner, Constants.MAX_INSTRUCTION_LENGTH)
                    .toLowerCase();
            switch (instruction) {
                case "-h":
                    printHelpMessage();
                    break;
                case "-c":
                    createAccount();
                    break;
                case "-s":
                    setInfo();
                    break;
                case "-i":
                    checkInfo();
                    break;
                case "-d":
                    deleteAccount();
                    break;
                case "-di":
                    checkDishInfo();
                    break;
                case "-ds":
                    checkDishSales();
                    break;
                case "-cf":
                    checkFavoriteNum();
                    break;
                case "-q":
                    User_interface.userLogout();
                    break;
                case "-exit":
                    exitSystem();
                    break;
                default:
                    System.out.println("非法指令!");
            }

        }
    }

    /**
     * 查询某菜品销量
     */
    private void checkDishSales() {
        try {
            checkDishInfo();
            System.out.println("请输入想要查询的菜品ID(输入-q退出)：");
            String inputDishId = User_interface.getStringWithLengthLimit(scanner, Constants.MAX_SHORT_STRING_LENGTH);
            if ("-q".equals(inputDishId)) {
                return;
            }
            ResultSet resultSet = Table_handler.isItemExists(connection, "Dishes", inputDishId, "dish_id", true);
            if (resultSet.next()) {
                int totalDishSales = Table_handler.getTotalDishSales(connection, Integer.parseInt(inputDishId));
                System.out.println("此菜品总销量为" + totalDishSales);
            } else {
                System.out.println("找不到此菜品ID对应的菜品！");
            }
        } catch (SQLException e) {
            System.out.println("查看菜品销量时出错");
        } catch (NumberFormatException e) {
            System.err.println("菜品ID只能由数字组成！");
        }
    }

    private void checkFavoriteNum() throws SQLException {
        String[] merchantIds = Table_handler.getAllMerchantId(connection);
        if (merchantIds.length == 0) {
            return;
        }
        System.out.println("商家ID\t收藏数");
        int count = 0;
        for (String merchantId : merchantIds) {
            count++;
            int favoriteNum = Table_handler.getFavoriteNum(connection, merchantId);
            System.out.println(merchantId + "\t" + favoriteNum);
            if (count % Constants.MAX_DISPLAY_NUM == 0) {
                if (!User_interface.yesOrNo(scanner, "列表过长，是否继续显示（Y/N）？")) {
                    System.out.println("显示已中断");
                    break;
                }
            }
        }
    }

    private void checkDishInfo() {
        try {
            String output = Table_handler.selectAll(connection, "Dishes",
                    new String[] { "菜品ID", "商家ID", "名称", "价格", "分类", "描述",
                            "原料", "可能过敏原", "营养成分", "评分", "图片", "是否为招牌" });
            if (output.isEmpty()) {
                System.out.println("系统内暂无菜品");
            } else {
                System.out.println(output);
            }
        } catch (SQLException e) {
            System.out.println("显示系统中所有菜品信息时出错");
        }
    }

    private int getUserType(String accountIdForCheck) throws SQLException {
        ResultSet rs = Table_handler.isItemExists(connection, "Users", accountIdForCheck,
                "account", true);
        int userType = -1;
        if (rs.next()) {
            userType = rs.getInt("type");
        } else {
            System.out.println("未查询到账户讯息，请检查账户ID是否正确");
        }
        return userType;
    }

    private void checkInfo() throws SQLException {
        System.out.print("请输入所需查询的账号:\t");
        String accountIdForCheck = User_interface.getStringWithLengthLimit(scanner, Constants.MAX_SHORT_STRING_LENGTH);
        int userType = getUserType(accountIdForCheck);
        if (userType == -1) {
            return;
        }
        System.out.println("该账号为" + User_type.getByCode(userType));
        String tableName;
        String[] columnNames;
        try {
            switch (userType) {
                case Constants.ADMIN:
                    System.out.println("管理员账号无详细信息，已自动退出");
                    return;
                case Constants.MERCHANT:
                    tableName = "Merchants";
                    columnNames = new String[] { "账号", "店铺名称", "店铺评分", "店铺位置" };
                    break;
                case Constants.CUSTOMER:
                    tableName = "Customers";
                    columnNames = new String[] { "账号", "用户昵称", "性别", "工号（学号）" };
                    break;
                default:
                    throw new UnknownUserTypeException(userType);
            }

            String output = Table_handler.selectOneRow(connection, tableName, accountIdForCheck, columnNames);
            if (output.isEmpty()) {
                System.out.println("该账户未创建信息");
            } else {
                System.out.print(output);
            }
        } catch (UnknownUserTypeException e) {
            System.out.println(e.getMessage());
        }

    }

    public void createAccount() {
        boolean accountCreated = false;
        while (!accountCreated) {
            try {
                System.out.println("请输入新账号密码及权限组(0: 管理员 1: 食堂商户 2: 顾客)：");
                System.out.print("新账号:\t");
                String accountId = User_interface.getStringWithLengthLimit(scanner, Constants.MAX_SHORT_STRING_LENGTH);
                System.out.print("新密码:\t");
                String password = User_interface.getStringWithLengthLimit(scanner, Constants.MAX_SHORT_STRING_LENGTH);
                System.out.print("权限组:\t");
                ArrayList<String> validInput = new ArrayList<>(Arrays.asList("0", "1", "2"));
                String type = User_interface.getValidatedInput(scanner, validInput);
                String[] newAccountData = { accountId, password, type };
                Table_handler.insertData(connection, "Users", newAccountData);
                accountCreated = true;
                System.out.println("账号成功创建");
            } catch (SQLException e) {
                System.out.println("账号创建失败，可能是因为账号已存在，请重新输入");
            }
        }
    }

    public void deleteAccount() {
        try {
            System.out.println("请输入需要删除的用户id(输入-q退出)：");
            String inputAccountId = User_interface.getStringWithLengthLimit(scanner, Constants.MAX_SHORT_STRING_LENGTH);
            if ("-q".equals(inputAccountId)) {
                return;
            }
            // 不可删除admin账号或者此管理员自己的账号
            if ("admin".equals(inputAccountId) || accountId.equals(inputAccountId)) {
                System.out.println("不可删除admin账号或者此管理员自己的账号！");
                return;
            }
            // 检查此用户是否存在
            ResultSet userRs = Table_handler.isItemExists(connection, "Users", inputAccountId, "account", true);
            if (!userRs.next()) {
                System.out.println("该账号不存在！请检查账号是否正确");
                return;
            }
            Table_handler.deleteData(connection, "Users", new String[] { inputAccountId }, new String[] { "account" });
            System.out.println("账号" + inputAccountId + "成功删除");
        } catch (SQLException e) {
            System.out.println("账号删除失败：" + e.getMessage());
        }
    }

    private void setInfo() throws SQLException {
        boolean dataSet = false;
        System.out.print("请输入所需设置信息的账号:\t");
        String inputAccountId = User_interface.getStringWithLengthLimit(scanner, Constants.MAX_SHORT_STRING_LENGTH);
        int userType = getUserType(inputAccountId);
        String tableName;
        String[] columnNames;
        try {
            while (!dataSet && userType != -1) {

                switch (userType) {
                    case Constants.ADMIN:
                        System.out.println("该账号为管理员，无需设置信息");
                        dataSet = true;
                        break;
                    case Constants.MERCHANT:
                        tableName = "Merchants";
                        System.out.println("请输入信息：");
                        System.out.print("店铺名称:\t");
                        String merchantName = User_interface.getValidIdentifier(scanner,
                                Constants.MAX_SHORT_STRING_LENGTH);
                        System.out.print("店铺地址:\t");
                        String serviceArea = User_interface.getValidIdentifier(scanner, Constants.MAX_COMMENT_LENGTH);

                        columnNames = new String[4];
                        columnNames[0] = inputAccountId;
                        columnNames[1] = merchantName;
                        columnNames[2] = "0.0";// rating，后续要再修改
                        columnNames[3] = serviceArea;

                        String merchantInfo = Table_handler.selectOneRow(connection,
                                tableName, inputAccountId, columnNames);
                        if (merchantInfo.isEmpty()) {
                            Table_handler.insertData(connection, "Merchants", columnNames);
                        } else {
                            Table_handler.update(connection, "Merchants", inputAccountId, "merchant_id",
                                    new String[] { "name", "service_area" },
                                    new String[] { merchantName, serviceArea });
                        }

                        dataSet = true;
                        System.out.println("信息设置成功");
                        break;
                    case Constants.CUSTOMER:
                        tableName = "Customers";
                        columnNames = new String[] { "账号", "用户昵称", "性别", "工作数" };
                        System.out.println("请输入信息：");
                        System.out.print("用户昵称:\t");
                        String customerName = User_interface.getValidIdentifier(scanner,
                                Constants.MAX_SHORT_STRING_LENGTH);
                        System.out.print("性别（男:" + Constants.MALE + " 女:" + Constants.FEMALE + " 其他:" + Constants.OTHERS
                                + "）:\t");
                        int gender = User_interface.getInt(scanner, Constants.OTHERS);
                        System.out.print("学号:\t");
                        String jobNumber = User_interface.getStringWithLengthLimit(scanner,
                                Constants.MAX_SHORT_STRING_LENGTH);

                        String[] customerData = new String[4];
                        customerData[0] = inputAccountId;
                        customerData[1] = customerName;
                        customerData[2] = String.valueOf(gender);
                        customerData[3] = jobNumber;

                        String customerInfo = Table_handler.selectOneRow(connection, tableName, inputAccountId,
                                columnNames);
                        if (customerInfo.isEmpty()) {
                            Table_handler.insertData(connection, "Customers", customerData);
                        } else {
                            Table_handler.update(connection, "Customers", inputAccountId, "customer_id",
                                    new String[] { "name", "gender", "job_number" },
                                    new String[] { customerName, String.valueOf(gender), jobNumber });
                        }

                        dataSet = true;
                        System.out.println("信息设置成功");
                        break;
                    default:
                        throw new UnknownUserTypeException(userType);
                }
            }
        } catch (SQLException e) {
            System.out.println("信息设置时失败");
        } catch (UnknownUserTypeException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void printHelpMessage() {
        System.out.println("-h \t显示本列表");
        System.out.println("-c \t创建账户");
        System.out.println("-i \t查询账户信息");
        System.out.println("-s \t设置账户信息");
        System.out.println("-d \t删除账户信息");
        System.out.println("-di \t查询所有菜品信息");
        System.out.println("-ds \t查询某菜品总销量");
        System.out.println("-cf \t查询所有商家的收藏情况");
        System.out.println("-q \t退出账号");
        System.out.println("-exit \t退出程序");
        System.out.println("以上所有指令大小写不分，且对于名称的搜索支持关键词查询");
    }

}
