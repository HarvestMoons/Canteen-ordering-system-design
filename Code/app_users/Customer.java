package app_users;

import java.sql.*;
import java.util.*;

import app_operation.SystemState;
import app_operation.User_interface;
import customExceptions.MenuNotCreatedException;
import customExceptions.QuitOrderProcessException;
import database_operation.Table_handler;
import utility.Constants;

public class Customer extends User {

    public Customer(Connection connection, Scanner scanner, String accountId) {
        super(connection, scanner, accountId);
    }

    @Override
    public void forceInfoSet() throws SQLException {
        ResultSet resultSet = Table_handler.isItemExists(connection, "Customers", accountId, "customer_id", true);
        if (!resultSet.isBeforeFirst()) {
            System.out.println("账户信息未设置，请先设置账户信息");
            setInfo(true);
        }
    }

    @Override
    public void printHelpMessage() {
        System.out.println("-h \t显示本列表");
        System.out.println("-i \t查询本账户信息");
        System.out.println("-s \t设置本账户信息");
        System.out.println("-fd \t收藏喜欢的菜品");
        System.out.println("-fm \t收藏喜欢的商家");
        System.out.println("-sm \t搜索特定商家信息");
        System.out.println("-sd \t搜索特定菜品信息");
        System.out.println("-o \t预订餐厅和点餐");
        System.out.println("-so \t展示所有与自己相关的订单");
        System.out.println("-df \t取消收藏菜品");
        System.out.println("-co \t评价订单");
        System.out.println("-q \t退出账号");
        System.out.println("-exit \t退出程序");
        System.out.println("以上所有指令大小写不分，且对于名称的搜索支持关键词查询");
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
                case "-s":
                    setInfo(false);
                    break;
                case "-i":
                    checkInfo();
                    break;
                case "-fd":
                    addFavoriteDish();
                    break;
                case "-fm":
                    searchAndAddFavoriteMerchant();
                    break;
                case "-sm":
                    searchMerchant();
                    break;
                case "-sd":
                    searchDish(null);
                    break;
                case "-o":
                    order();
                    break;
                case "-so":
                    showAllOrders();
                    break;
                case "-co":
                    comment();
                    break;
                case "-df":
                    deleteFavoriteDish();
                    break;
                case "-q":
                    User_interface.userLogout();
                    break;
                case "-exit":
                    exitSystem();
                    break;
                default:
                    System.out.println("非法指令！");
            }

        }
    }

    private void deleteFavoriteMerchant(String merchantId) {
        try {
            String tableName = "Favorite_Merchants";
            ResultSet resultSet = Table_handler.isItemExists(connection, tableName,
                    accountId, "customer_id", merchantId, "merchant_id", true);
            if (Table_handler.getResultSetLength(resultSet) == 0) {
                System.out.println("您未收藏此商家！");
                return;
            }
            String[] primaryKeyColumnName = new String[] { "customer_id", "merchant_id" };
            String[] primaryKeyValue = new String[] { accountId, merchantId };
            Table_handler.deleteData(connection, tableName, primaryKeyValue, primaryKeyColumnName);
            System.out.println("该商家不再是您的收藏");
        } catch (SQLException e) {
            System.out.println("删除收藏商家时出错");
        }

    }

    private void comment() throws SQLException {
        ResultSet orderResultSet = Table_handler.showAllRelatedOrders(connection, accountId, "customer_id", true);
        if (orderResultSet == null) {
            System.out.println("退出评价");
            return;
        }
        int count = Table_handler.getResultSetLength(orderResultSet);
        int orderId = -1;
        if (count == 0) {
            System.out.println("退出评价");
            return;
        } else if (count == 1) {
            if (User_interface.yesOrNo(scanner, "是否评价该订单(y/n)")) {
                orderResultSet.next();
                orderId = orderResultSet.getInt("order_id");
            }
        } else {
            System.out.println("请选择你想评价的订单：");
            orderResultSet.absolute(count + 1 - User_interface.getInt(scanner, count));
            orderId = orderResultSet.getInt("order_id");
        }
        boolean commentedMerchant = commentMerchant(orderResultSet);
        boolean commentedDish = commentDish(orderResultSet);
        if (commentedMerchant || commentedDish) {
            Table_handler.update(connection, "orders", orderId, "order_id",
                    new String[] { "order_state" }, new Integer[] { Constants.COMMENTED });
        }
    }

    private boolean commentMerchant(ResultSet orderResultSet) {
        try {
            if (!User_interface.yesOrNo(scanner, "请问是否需要评价商家(y/n)")) {
                return false;
            }
            String merchantId = orderResultSet.getString("merchant_id");
            System.out.println("请为商家评分(1~5)");
            int rating = User_interface.getInt(scanner, 5);
            System.out.println("请写出您对商家的评价");
            String comment = User_interface.getStringWithLengthLimit(scanner, Constants.MAX_COMMENT_LENGTH);
            Table_handler.insertData(connection, "merchants_comments",
                    new Object[] { accountId, merchantId, comment, rating });
            float newRating = Table_handler.getNewRating(connection, "merchants_comments",
                    "merchant_id", orderResultSet.getString("merchant_id"));
            if (newRating != 0) {
                Table_handler.update(connection, "Merchants", orderResultSet.getString("merchant_id"),
                        "merchant_id", new String[] { "rating" }, new Float[] { newRating });
            }
            return true;
        } catch (SQLException e) {
            System.out.println("评价商家出错，可能已经对该菜品进行过评价");
            return false;
        }

    }

    // TODO:以下需要加上回滚
    // TODO:是否需要处理已有过评价，再评进行覆盖
    private boolean commentDish(ResultSet orderResultSet) {
        boolean commented = false;
        try {
            ResultSet dishedResultSet = Table_handler.showDishesInOrder(connection, orderResultSet);
            int count = Table_handler.getResultSetLength(dishedResultSet);
            int[] selectedDishes = User_interface.getValidDishSelection(scanner, count);
            for (int selectedDish : selectedDishes) {
                dishedResultSet.absolute(selectedDish);
                System.out.println("请为" + Table_handler.getNameById(connection, "Dishes", "dish_id",
                        "name", String.valueOf(dishedResultSet.getInt("dish_id"))) + "评分(1~5)");
                int rating = User_interface.getInt(scanner, 5);
                System.out.println("请写出您对该菜品的评价");
                String comment = User_interface.getStringWithLengthLimit(scanner, Constants.MAX_COMMENT_LENGTH);
                Table_handler.insertData(connection, "dishes_comments",
                        new Object[] { accountId, dishedResultSet.getInt("dish_id"), comment, rating });
                float newRating = Table_handler.getNewRating(connection, "dishes_comments",
                        "dish_id", dishedResultSet.getString("dish_id"));
                if (newRating != 0) {
                    Table_handler.update(connection, "Dishes", dishedResultSet.getString("dish_id"),
                            "dish_id", new String[] { "rating" }, new Float[] { newRating });
                }
            }
            commented = true;
        } catch (QuitOrderProcessException e) {
            System.out.println("退出评价");
        } catch (SQLException e) {
            System.out.println("评价菜品出错，可能已经对该菜品进行过评价");
        }
        return commented;

    }

    private void checkInfo() throws SQLException {
        String tableName = "Customers";
        String[] columnNames = new String[] { "账号", "用户昵称", "性别", "学号" };
        String info = Table_handler.selectOneRow(connection, tableName, accountId, columnNames);
        System.out.print(info);
    }

    private void setInfo(boolean isFirstSet) {
        try {
            System.out.println("请输入信息：");
            System.out.print("用户昵称:\t");
            String name = User_interface.getValidIdentifier(scanner, Constants.MAX_SHORT_STRING_LENGTH);
            System.out.print("性别（男:" + Constants.MALE + " 女:" + Constants.FEMALE + " 其他:" + Constants.OTHERS + "）:\t");
            int gender = User_interface.getInt(scanner, Constants.OTHERS);
            System.out.print("学号:\t");
            String jobNumber = User_interface.getStringWithLengthLimit(scanner, Constants.MAX_SHORT_STRING_LENGTH);
            String[] data = new String[4];
            data[0] = accountId;
            data[1] = name;
            data[2] = String.valueOf(gender);
            data[3] = jobNumber;
            if (isFirstSet) {
                Table_handler.insertData(connection, "Customers", data);
                System.out.println("信息设置成功，之后您可以再修改这些信息");
            } else {
                Table_handler.update(connection, "Customers", accountId, "customer_id",
                        new String[] { "name", "gender", "job_number" },
                        new String[] { name, String.valueOf(gender), jobNumber });
                System.out.println("信息修改成功");
            }

        } catch (SQLException e) {
            System.out.println("信息设置失败，已自动返回主界面");
            if (isFirstSet) {
                User_interface.userLogout();
            }
        }

    }

    public void addFavoriteDish() throws SQLException {
        int dishId = chooseDishFromAll("收藏");
        if (dishId == -1) {
            System.out.println("已自动退出");
            return;
        }
        // 检查是否收藏过，若有，直接返回
        if (Table_handler.isItemExists(connection, "Favorite_Dishes", String.valueOf(dishId),
                "dish_id", accountId, "customer_id", true).next()) {
            System.out.println("您已经收藏过此菜品！");
            return;
        }
        String[] data = { accountId, String.valueOf(dishId) };
        Table_handler.insertData(connection, "Favorite_Dishes", data);
        System.out.println("菜品收藏成功");
    }

    public void deleteFavoriteDish() throws SQLException {
        int dishId = chooseDishFromAll("取消收藏");
        if (dishId == -1) {
            System.out.println("已自动退出");
            return;
        }
        // 检查是否收藏过，若有，直接返回
        if (!Table_handler.isItemExists(connection, "Favorite_Dishes", String.valueOf(dishId),
                "dish_id", accountId, "customer_id", true).next()) {
            System.out.println("您未收藏此菜品");
            return;
        }
        Table_handler.deleteData(connection, "Favorite_Dishes",
                new String[] { accountId, String.valueOf(dishId) }, new String[] { "customer_id", "dish_id" });
        System.out.println("菜品取消收藏成功");
    }

    public void addFavoriteDishFromMerchant(String merchantId) throws SQLException {
        int dishId = chooseDishFromMerchant("收藏", merchantId);
        if (dishId == -1) {
            System.out.println("已自动退出");
            return;
        }
        // 检查是否收藏过，若有，直接返回
        if (Table_handler.isItemExists(connection, "Favorite_Dishes", String.valueOf(dishId),
                "dish_id", accountId, "customer_id", true).next()) {
            System.out.println("您已经收藏过此菜品！");
            return;
        }
        String[] data = { accountId, String.valueOf(dishId) };
        Table_handler.insertData(connection, "Favorite_Dishes", data);
        System.out.println("菜品收藏成功");
    }

    private void searchAndAddFavoriteMerchant() throws SQLException {
        String merchantId = chooseMerchant("收藏");
        if (merchantId != null) {
            addFavoriteMerchant(merchantId);
        }
    }

    private void addFavoriteMerchant(String merchantId) throws SQLException {
        // 检查是否收藏过，若有，直接返回
        if (Table_handler.isItemExists(connection, "Favorite_Merchants", String.valueOf(merchantId),
                "merchant_id", accountId, "customer_id", true).next()) {
            System.out.println("您已经收藏过此商家！");
            return;
        }
        String[] data = { accountId, String.valueOf(merchantId) };
        Table_handler.insertData(connection, "Favorite_Merchants", data);
        System.out.println("商家收藏成功");
    }

    private void searchMerchant() throws SQLException {
        String merchantId = chooseMerchant("搜索并进入");
        if (merchantId != null) {
            try {
                showMenu(merchantId);
            } catch (MenuNotCreatedException e) {
                System.out.println(e.getMessage());
                return;
            }
            boolean isQuitMerchant = false;
            printHelpMessageWhenOrdering();
            while (!isQuitMerchant) {
                System.out.println("请选择需要进行的操作(输入8查看所有指令)：");
                try {
                    switch (User_interface.getInt(scanner, 9)) {
                        case 1:
                            showMenu(merchantId);
                            break;
                        case 2:
                            createNewOrder(merchantId);
                            break;
                        case 3:
                            showAllComments(merchantId);
                            break;
                        case 4:
                            addFavoriteMerchant(merchantId);
                            break;
                        case 5:
                            searchDish(merchantId);
                            break;
                        case 6:
                            addFavoriteDishFromMerchant(merchantId);
                            break;
                        case 7:
                            showSpecialtyDishes(merchantId);
                            break;
                        case 8:
                            printHelpMessageWhenOrdering();
                            break;
                        case 9:
                            deleteFavoriteMerchant(merchantId);
                            break;
                        case 10:
                            isQuitMerchant = true;
                            break;
                        default:
                    }
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }

    private void printHelpMessageWhenOrdering() {
        System.out.println("可选指令如下：");
        System.out.println("1 显示菜单详情");
        System.out.println("2 下单");
        System.out.println("3 查看商家评价");
        System.out.println("4 收藏该商家");
        System.out.println("5 查询菜品详细信息");
        System.out.println("6 收藏某一菜品");
        System.out.println("7 查看商家主打菜");
        System.out.println("8 显示指令列表");
        System.out.println("9 取消收藏该商家");
        System.out.println("10 退出该商家");

    }

    /** 显示招牌菜 */
    private void showSpecialtyDishes(String merchantId) {
        ResultSet specialtyRs = Table_handler.getSpecialtyDishesByMerchantId(connection, merchantId);
        if (specialtyRs == null) {
            System.out.println("该商家未设置任何特色菜！");
            return;
        }
        try {
            while (specialtyRs.next()) {
                System.out.println("菜品名：" + specialtyRs.getString("name")
                        + "\t价格：" + specialtyRs.getFloat("price")
                        + "\t菜品类型：" + specialtyRs.getString("type"));
            }
        } catch (SQLException e) {
            System.out.println("展示特色菜时出错");
        }
    }

    private void searchDish(String merchantId) throws SQLException {
        int dishId;
        if (merchantId == null) {
            dishId = chooseDishFromAll("查询详细信息");
        } else {
            dishId = chooseDishFromMerchant("查询详细信息", merchantId);
        }
        if (dishId == -1) {
            return;
        }
        Table_handler.showDishDetail(connection, dishId);
        if (User_interface.yesOrNo(scanner, "请问是否查看菜品评论(y/n)")) {
            Table_handler.showAllComments(connection, scanner, "dishes_comments", dishId, "dish_id");
        }
    }

    /** 查询菜品 返回-1是不操作 */
    private int chooseDishFromAll(String info) throws SQLException {
        System.out.println("请输入想要" + info + "的菜品名:");
        String dishName = User_interface.getValidIdentifier(scanner, Constants.MAX_SHORT_STRING_LENGTH);
        ResultSet resultSet;
        int dishId = -1;
        resultSet = Table_handler.isItemExists(connection, "Dishes", dishName, "name", false);
        int rows = Table_handler.getResultSetLength(resultSet);
        if (rows == 0) {
            System.out.println("菜品不存在！" + info + "失败");
        } else if (rows == 1) {
            System.out.println("符合条件的菜品信息如下：");
            resultSet.next();
            System.out.println("商户名称：" + Table_handler.getNameById(connection, "Merchants",
                    "merchant_id", "name", resultSet.getString("merchant_id"))
                    + "\t菜品名：" + resultSet.getString("name") + "\t价格：" + resultSet.getFloat("price"));
            if (User_interface.yesOrNo(scanner, "您想" + info + "该菜品吗(y/n)")) {
                dishId = resultSet.getInt("dish_id");
            }
        } else {
            System.out.println("符合条件的菜品信息如下：");
            int count = 0;
            while (resultSet.next()) {
                count++;
                System.out.println("菜品" + count +
                        "\t商户名称：" + Table_handler.getNameById(connection, "Merchants",
                                "merchant_id", "name", resultSet.getString("merchant_id"))
                        + "\t菜品名：" + resultSet.getString("name") + "\t价格：" + resultSet.getFloat("price"));
            }
            System.out.println("请输入对应的菜品序号：");
            // 跳转到用户选择的那个菜品
            resultSet.absolute(User_interface.getInt(scanner, count));
            dishId = resultSet.getInt("dish_id");
        }
        return dishId;
    }

    /** 查询菜品 返回-1是不操作 */
    private int chooseDishFromMerchant(String info, String merchantId) throws SQLException {
        System.out.println("请输入想要" + info + "的菜品名:");
        String dishName = User_interface.getValidIdentifier(scanner, Constants.MAX_SHORT_STRING_LENGTH);
        ResultSet resultSet;
        int dishId = -1;
        resultSet = Table_handler.isItemExists(
                connection, "Dishes", dishName, "name", merchantId, "merchant_id", false);
        resultSet.last();// 移到最后一行
        int rows = resultSet.getRow();
        resultSet.beforeFirst();// 移到初始位置
        if (rows == 0) {
            System.out.println("菜品不存在！" + info + "失败");
        } else if (rows == 1) {
            System.out.println("符合条件的菜品信息如下：");
            resultSet.next();
            System.out.println("菜品名：" + resultSet.getString("name") + "\t价格：" + resultSet.getFloat("price"));
            if (User_interface.yesOrNo(scanner, "您想" + info + "该菜品吗(y/n)")) {
                dishId = resultSet.getInt("dish_id");
            }
        } else {
            System.out.println("符合条件的菜品信息如下：");
            int count = 0;
            while (resultSet.next()) {
                count++;
                System.out.println("菜品" + count +
                        "\t菜品名：" + resultSet.getString("name") + "\t价格：" + resultSet.getFloat("price"));
            }
            System.out.println("请输入对应的菜品序号：");
            // 跳转到用户选择的那个菜品
            resultSet.absolute(User_interface.getInt(scanner, count));
            dishId = resultSet.getInt("dish_id");
        }
        return dishId;
    }

    private void showAllComments(String merchantId) {
        Table_handler.showAllComments(connection, scanner, "merchants_comments", merchantId, "merchant_id");
    }

    /** 查询商家 返回null是不操作 */
    private String chooseMerchant(String info) throws SQLException {
        ResultSet resultSet;
        System.out.println("请输入想要" + info + "的商家名称:");
        String merchantName = User_interface.getStringWithLengthLimit(scanner, Constants.MAX_SHORT_STRING_LENGTH);
        resultSet = Table_handler.isItemExists(connection, "Merchants",
                merchantName, "name", false);
        int rows = Table_handler.getResultSetLength(resultSet);
        String merchantId = null;
        if (rows == 0) {
            System.out.println("符合条件的商家不存在！");
        } else if (rows == 1) {
            System.out.println("符合条件的商家信息如下：");
            resultSet.next();
            String name = resultSet.getString("name");
            float rating = resultSet.getFloat("rating");
            String serviceArea = resultSet.getString("service_area");
            System.out.println("商家名称：" + name +
                    "\t商家评分：" + rating + "\t服务区域：" + serviceArea);
            if (User_interface.yesOrNo(scanner, "您想" + info + "该商家吗(y/n)")) {
                merchantId = resultSet.getString("merchant_id");
            }
        } else {
            System.out.println("符合条件的商家信息如下：");
            int count = 0;
            ArrayList<String> validInput = new ArrayList<>();
            while (resultSet.next()) {
                count++;
                validInput.add(String.valueOf(count));
                String name = resultSet.getString("name");
                float rating = resultSet.getFloat("rating");
                String serviceArea = resultSet.getString("service_area");
                System.out.println(count + "\t商家名称：" + name +
                        "\t商家评分：" + rating + "\t服务区域：" + serviceArea);
            }
            System.out.println("输入您想" + info + "的商家序号，若不需要，输入-q退出");
            validInput.add("-q");
            String userInput = User_interface.getValidatedInput(scanner, validInput);
            if ("-q".equals(userInput)) {
                System.out.println("已自动退出");
                return null;
            }
            resultSet.absolute(Integer.parseInt(userInput));
            merchantId = resultSet.getString("merchant_id");
        }
        return merchantId;
    }

    private void order() throws SQLException {
        System.out.println("您希望在哪里点餐？");
        String merchantId = null;
        while (merchantId == null) {
            merchantId = chooseMerchant("点单");
        }
        createNewOrder(merchantId);
    }

    private void createNewOrder(String merchantId) {
        boolean transactionSuccess = false;
        int orderId = 0;
        try {
            // 禁用自动提交，开始一个事务
            connection.setAutoCommit(false);

            String insertIntoOrders = "INSERT INTO Orders (customer_id, merchant_id, order_state) VALUES (?, ?, ?)";
            PreparedStatement preparedStatement = connection.prepareStatement(insertIntoOrders,
                    Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, accountId);
            preparedStatement.setString(2, merchantId);
            preparedStatement.setInt(3, Constants.UNFINISHED);
            preparedStatement.executeUpdate();
            ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                orderId = generatedKeys.getInt(1);
                orderDishes(merchantId, orderId);
                transactionSuccess = true;
            }
        } catch (Exception e) {
            System.err.println("点餐过程出错");
            // 语句执行异常,在catch中进行回滚事务
            try {

                connection.rollback();
            } catch (SQLException ex) {
                System.err.println("事务回滚出错");
            }
        } finally {
            // 在关闭资源之前进行提交,无论是否执行回滚,都需要进行提交,所以将提交放在finally里面
            try {
                // 提交事务,重新启用自动提交
                connection.commit();
                connection.setAutoCommit(true);
                // 在事务成功提交之后提醒用户点餐成功
                if (transactionSuccess) {
                    System.out.println("点餐成功！订单号" + orderId);
                }
            } catch (SQLException e) {
                System.err.println("点餐时事务提交出错");
            }
        }
    }

    private void orderDishes(String merchantId, int orderId)
            throws SQLException, MenuNotCreatedException, QuitOrderProcessException {
        try (ResultSet resultSet = showMenu(merchantId)) {
            int count = Table_handler.getResultSetLength(resultSet);

            int[] selectedDishes = User_interface.getValidDishSelection(scanner, count);
            int[] numOfDishes = User_interface.getValidDishSelection(scanner,
                    Constants.MAX_DISH_ORDER_AMOUNT, selectedDishes.length);

            for (int i = 0; i < selectedDishes.length; i++) {
                int selectedDish = selectedDishes[i];
                int numOfDish = numOfDishes[i];
                resultSet.absolute(selectedDish);
                int dishId = resultSet.getInt("dish_id");
                String[] data = { String.valueOf(orderId), String.valueOf(dishId), String.valueOf(numOfDish) };
                Table_handler.insertData(connection, "Dishes_Ordered", data);
            }
        } catch (MenuNotCreatedException e) {
            throw new MenuNotCreatedException("该商家还未创建菜单，无法点单，已自动退出");
        }

    }

    private ResultSet showMenu(String merchantId) throws SQLException, MenuNotCreatedException {
        // 查询视图，按 merchant_id 过滤
        PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT * FROM simpleDishView WHERE merchant_id = ?",
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY);
        preparedStatement.setString(1, merchantId);
        ResultSet resultSet = preparedStatement.executeQuery();
        int count = 0;
        int rows = Table_handler.getResultSetLength(resultSet);
        if (rows == 0) {
            throw new MenuNotCreatedException("该商家还未创建菜单，已自动退出");
        }
        System.out.println("菜单如下：");
        while (resultSet.next()) {
            count++;
            System.out.println(count +
                    "\t菜品名: " + resultSet.getString("name") +
                    "\t价格（元）: " + resultSet.getFloat("price") +
                    "\t菜品类型: " + resultSet.getString("type"));
            if (count % Constants.MAX_DISPLAY_NUM == 0) {
                if (!User_interface.yesOrNo(scanner, "列表过长，是否继续显示（Y/N）？")) {
                    System.out.println("显示已中断");
                    break;
                }
            }
        }
        System.out.println("菜单显示完毕");
        return resultSet;
    }

    // 展示所有订单
    private void showAllOrders() {
        Table_handler.showAllRelatedOrders(connection, accountId, "customer_id", false);
    }
}
