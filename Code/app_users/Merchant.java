package app_users;

import java.sql.*;
import java.util.*;

import app_operation.SystemState;
import app_operation.User_interface;
import database_operation.Table_handler;
import utility.Constants;
import utility.Translator;

public class Merchant extends User {

    public Merchant(Connection connection, Scanner scanner, String accountId) {
        super(connection, scanner, accountId);
    }

    @Override
    public void forceInfoSet() throws SQLException {
        ResultSet resultSet = Table_handler.isItemExists(connection, "Merchants", accountId, "merchant_id", true);
        if (!resultSet.isBeforeFirst()) {
            System.out.println("账户信息未设置，请先设置账户信息");
            setInfo(true);
        }
    }

    @Override
    public void printHelpMessage() {
        System.out.println("-h \t显示本列表");
        System.out.println("-i \t查询本账户信息");
        System.out.println("-s \t修改本账户信息");
        System.out.println("-ad \t添加菜品");
        System.out.println("-as \t添加招牌菜");
        System.out.println("-ud \t修改菜品信息");
        System.out.println("-dd \t删除菜品信息");
        System.out.println("-m \t显示菜单");
        System.out.println("-o \t展示所有与自己相关的订单");
        System.out.println("-fo \t完成订单");
        System.out.println("-ds \t删除招牌菜");
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
                case "-i":
                    checkInfo();
                    break;
                case "-s":
                    setInfo(false);
                    break;
                case "-ad":
                    addDish();
                    break;
                case "-as":
                    showMenu(Constants.ADD_SPECIALTY);
                    break;
                case "-ud":
                    showMenu(Constants.MODIFY_DISHES);
                    break;
                case "-dd":
                    deleteDishes();
                    break;
                case "-m":
                    showMenu(Constants.NO_MORE_OPERATION);
                    break;
                case "-o":
                    Table_handler.showAllRelatedOrders(connection, accountId, "merchant_id", false);
                    break;
                case "-fo":
                    finishOrders();
                    break;
                case "-ds":
                    showMenu(Constants.DELETE_SPECIALTY);
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

    private void checkInfo() throws SQLException {
        String tableName = "Merchants";
        String[] columnNames = new String[] { "账号", "店铺名称", "店铺评分", "店铺位置" };
        String info = Table_handler.selectOneRow(connection, tableName, accountId, columnNames);
        System.out.print(info);
    }

    public void setInfo(boolean isFirstSet) {
        try {
            System.out.println("请输入信息：");
            System.out.print("店铺名称:\t");
            String name = User_interface.getValidIdentifier(scanner, Constants.MAX_SHORT_STRING_LENGTH);
            System.out.print("店铺地址:\t");
            String serviceArea = User_interface.getValidIdentifier(scanner, Constants.MAX_COMMENT_LENGTH);
            String[] data = new String[4];
            data[0] = accountId;
            data[1] = name;
            data[2] = "0.0";
            data[3] = serviceArea;
            if (isFirstSet) {
                Table_handler.insertData(connection, "Merchants", data);
                System.out.println("信息设置成功，之后您可以再修改这些信息");
            } else {
                Table_handler.update(connection, "Merchants", accountId, "merchant_id",
                        new String[] { "name", "service_area" }, new String[] { name, serviceArea });
                System.out.println("信息修改成功");
            }
        } catch (SQLException e) {
            System.out.println("信息设置失败，已自动返回主界面");
            User_interface.userLogout();
        }

    }

    private void addDish() {
        try {
            System.out.println("请输入新菜品信息：");
            System.out.print("菜品名称:\t");
            String name = User_interface.getValidIdentifier(scanner, Constants.MAX_SHORT_STRING_LENGTH);
            String price = User_interface.getDishPrice(scanner);
            System.out.print("菜品分类:\t");
            String type = User_interface.getStringWithLengthLimit(scanner, Constants.MAX_SHORT_STRING_LENGTH);
            System.out.print("菜品描述:\t");
            String description = User_interface.getStringWithLengthLimit(scanner, Constants.MAX_COMMENT_LENGTH);
            System.out.print("菜品原料:\t");
            String ingredients = User_interface.getStringWithLengthLimit(scanner, Constants.MAX_COMMENT_LENGTH);
            System.out.print("可能的过敏源:\t");
            String allergen = User_interface.getStringWithLengthLimit(scanner, Constants.MAX_COMMENT_LENGTH);
            System.out.print("营养成分:\t");
            String nutrition = User_interface.getStringWithLengthLimit(scanner, Constants.MAX_COMMENT_LENGTH);
            String[] dishData = { accountId, name, price, type,
                    description, ingredients, allergen, nutrition, "2.5", "no image" };
            int dishId = Table_handler.insertDish(connection, dishData);
            if (dishId == -1) {
                throw new SQLException();
            }
            System.out.println("新菜品添加成功！新菜品ID为" + dishId);
        } catch (SQLException e) {
            System.out.println("菜品添加失败，请重新输入");
        }

    }

    private void deleteDishes() {
        try {
            showMenu(Constants.NO_MORE_OPERATION);
            System.out.println("请输入想删除的菜品的ID(输入-q退出)：");
            String inputDishId = User_interface.getStringWithLengthLimit(scanner, Constants.MAX_SHORT_STRING_LENGTH);
            if ("-q".equals(inputDishId)) {
                return;
            }
            // 检查此菜品是否存在
            ResultSet dishRs = Table_handler.isItemExists(connection, "Dishes", inputDishId, "dish_id", true);
            if (!dishRs.next()) {
                System.out.println("该菜品不存在！请检查菜品ID是否正确");
                return;
            }
            Table_handler.deleteData(connection, "Dishes", new String[] { inputDishId }, new String[] { "dish_id" });
            System.out.println("菜品删除成功！");
        } catch (SQLException e) {
            System.out.println("菜品删除失败！");
        }

    }

    private void showMenu(int nextOperation) {
        try {
            int count = 0;
            // TYPE_SCROLL_INSENSITIVE 使得 ResultSet 可以前后滚动，
            // 而 CONCUR_UPDATABLE 使得 ResultSet 是可更新的
            Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_UPDATABLE);
            ResultSet rs = statement.executeQuery("SELECT * FROM Dishes;");
            if (Table_handler.getResultSetLength(rs) == 0) {
                System.out.println("当前没有任何菜品");
                return;
            }
            System.out.println("序号\t菜品ID\t菜品名称\t价格");
            while (rs.next()) {
                count++;
                System.out.printf("%d\t%d\t%s\t%.2f\n",
                        count, rs.getInt("dish_id"), rs.getString("name"), rs.getFloat("price"));
                if (count % Constants.MAX_DISPLAY_NUM == 0) {
                    if (!User_interface.yesOrNo(scanner, "列表过长，是否继续显示（Y/N）？")) {
                        System.out.println("菜单显示已中断");
                        break;
                    }
                }
            }

            switch (nextOperation) {
                case Constants.NO_MORE_OPERATION:
                    return;
                case Constants.ADD_SPECIALTY:
                    addSpecialtyDishes(rs, count);
                    break;
                case Constants.MODIFY_DISHES:
                    modifyDishes(rs, count);
                    break;
                case Constants.DELETE_SPECIALTY:
                    deleteSpecialtyDishes(rs, count);
                    break;
                default:
                    System.err.println("展示菜单时发现未定义的操作：" + nextOperation);
            }

        } catch (SQLException e) {
            System.out.println("菜单显示出错");
        }
    }

    // 添加招牌菜
    private void addSpecialtyDishes(ResultSet allDishesResultSet, int rsLength) {
        try {
            boolean isContinueAdding = true;
            ArrayList<Integer> addedDishesNum = new ArrayList<>();
            while (isContinueAdding) {
                System.out.println("请选择你想要添加作为本店招牌菜的菜品序号：");
                int userInput = User_interface.getInt(scanner, rsLength);
                allDishesResultSet.absolute(userInput);
                if (allDishesResultSet.getBoolean("is_specialty") || addedDishesNum.contains(userInput)) {
                    System.out.println("该菜品已经是招牌菜了");
                } else {
                    String dishName = allDishesResultSet.getString("name");
                    int dishId = allDishesResultSet.getInt("dish_id");
                    Table_handler.update(connection, "Dishes", dishId, "dish_id",
                            new String[] { "is_specialty" }, new Boolean[] { true });
                    System.out.println(dishName + "已成功添加为本店招牌菜！");
                    addedDishesNum.add(userInput);
                }
                isContinueAdding = User_interface.yesOrNo(scanner, "您想再添加其他招牌菜吗？(y/n)");
            }
        } catch (SQLException e) {
            System.out.println("添加招牌菜时出错，添加失败");
        }
    }

    private void deleteSpecialtyDishes(ResultSet allDishesResultSet, int rsLength) {
        try {
            boolean isContinueDeleting = true;
            ArrayList<Integer> deletedDishesNum = new ArrayList<>();
            while (isContinueDeleting) {
                System.out.println("请选择你想要删除的本店招牌菜的菜品序号：");
                int userInput = User_interface.getInt(scanner, rsLength);
                allDishesResultSet.absolute(userInput);
                if (!allDishesResultSet.getBoolean("is_specialty") || deletedDishesNum.contains(userInput)) {
                    System.out.println("该菜品不是招牌菜");
                } else {
                    String dishName = allDishesResultSet.getString("name");
                    int dishId = allDishesResultSet.getInt("dish_id");
                    Table_handler.update(connection, "Dishes", dishId, "dish_id",
                            new String[] { "is_specialty" }, new Boolean[] { false });
                    System.out.println(dishName + "已从本店招牌菜中移除！");
                    deletedDishesNum.add(userInput);
                }
                isContinueDeleting = User_interface.yesOrNo(scanner, "您想再删除其他招牌菜吗？(y/n)");
            }
        } catch (SQLException e) {
            System.out.println("删除招牌菜时出错，删除失败");
        }
    }

    public void modifyDishes(ResultSet allDishesResultSet, int rsLength) {
        try {
            boolean isContinueAdding = true;
            Map<Integer, String[]> numToDishName = Translator.getMapOfDishName();
            while (isContinueAdding) {
                System.out.println("请选择你想要修改信息的菜品序号：");
                int dishChoice = User_interface.getInt(scanner, rsLength);
                allDishesResultSet.absolute(dishChoice);
                User_interface.showDetailDishMessage(allDishesResultSet, numToDishName);
                System.out.println("请选择你想要修改信息的菜品条目序号(评分无法修改)：");
                int itemChoice = User_interface.getInt(scanner, 7);
                System.out.println("请输入修改后的信息：");
                // 此处取Constants.MAX_COMMENT_LENGTH，有可能超限
                String modifiedInfo = User_interface.getStringWithLengthLimit(scanner, Constants.MAX_COMMENT_LENGTH);
                String dishName = allDishesResultSet.getString("name");
                allDishesResultSet.updateString(numToDishName.get(itemChoice)[0], modifiedInfo);
                allDishesResultSet.updateRow();
                System.out.println(dishName + "的" + numToDishName.get(itemChoice)[1] + "已经被成功更新！");

                isContinueAdding = User_interface.yesOrNo(scanner, "您想再修改其他菜品吗？(y/n)");
            }
        } catch (SQLException e) {
            System.out.println("修改菜品信息时出错，修改失败");
        }
    }

    public void finishOrders() {
        try {
            ResultSet orderResultSet = Table_handler.getUnfinishedOrders(connection, accountId, "merchant_id");
            if (orderResultSet == null) {
                return;
            }
            Table_handler.showSingleOrder(connection, orderResultSet);
            int rows = Table_handler.getResultSetLength(orderResultSet);
            if (rows != 0) {
                System.out.println("输入未完成的订单的序号，即可将其标记为已出餐");
                orderResultSet.absolute(rows + 1 - User_interface.getInt(scanner, rows));
                orderResultSet.updateInt("order_state", Constants.FINISHED);
                orderResultSet.updateRow();
                System.out.println("该订单已出餐，待用户上线时会自动通知");
            }

        } catch (SQLException e) {
            System.err.println("商家完成订单时出错");
        }
    }

}
