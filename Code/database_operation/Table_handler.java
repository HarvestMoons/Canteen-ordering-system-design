package database_operation;

import app_operation.User_interface;
import customExceptions.IllegalOrderStateChangeException;
import customExceptions.OrderNotFoundException;
import customExceptions.OrderStateUndefinedExpection;
import utility.Constants;
import utility.Translator;

import java.sql.*;
import java.util.Scanner;

public class Table_handler {

    public static void dropAllTables(Connection connection) {
        try (Statement stmt = connection.createStatement()) {
            DatabaseMetaData metaData = connection.getMetaData();
            // 禁用外键约束
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
            // 查询所有表名
            ResultSet rs = metaData.getTables(myConnection.SCHEMA, null, "%", new String[] { "TABLE" });
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                String dropTableSql = "DROP TABLE IF EXISTS " + tableName;
                stmt.executeUpdate(dropTableSql);
            }
            // 启用外键约束
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
            System.out.println("\n=====所有表已被弃置=====\n");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void databaseInitialize(Connection connection) throws SQLException {
        Statement stmt = connection.createStatement();
        stmt.executeUpdate("create table if not exists Users(" +
                "account varchar(20) NOT NULL primary key," +
                "password varchar(20) NOT NULL," +
                "type ENUM('0', '1', '2') NOT NULL" +
                ");");
        try {
            String[] firstAdminData = { "admin", "1234", "0" };
            Table_handler.insertData(connection, "Users", firstAdminData);
            System.out.println("创建初始管理员成功");
        } catch (SQLException e) {
            System.out.println("初始管理员已存在，不再创建");
        }
        stmt.executeUpdate("create table if not exists Merchants(" +
                "merchant_id varchar(20) NOT NULL," +
                "name varchar(20) NOT NULL," +
                "rating float," +
                "service_area varchar(256) NOT NULL," +
                "foreign key(merchant_id) references Users(account) on delete cascade," +
                "primary key(merchant_id)," +
                "index idx_name (name)" + // 在 name 列上创建索引
                ");");
        stmt.executeUpdate("create table if not exists Dishes(" +
                "dish_id int NOT NULL AUTO_INCREMENT," +
                "merchant_id varchar(20) NOT NULL," +
                "name varchar(20) NOT NULL," +
                "price float(6,2) NOT NULL," +
                "type varchar(20)," +
                "description varchar(256)," +
                "ingredients varchar(256)," +
                "possible_allergens varchar(256)," +
                "nutrition varchar(256)," +
                "rating float," +
                "image varchar(256)," +
                "is_specialty boolean NOT NULL DEFAULT FALSE," +
                "foreign key(merchant_id) references Merchants(merchant_id) on delete cascade," +
                "primary key(dish_id)," +
                "index idx_dish_id (dish_id)," +
                "CHECK (price >= 0)," +
                "CHECK (rating >= 1 AND rating <= 5)" +
                ");");
        stmt.executeUpdate("create table if not exists Customers(" +
                "customer_id varchar(20) NOT NULL primary key," +
                "name varchar(20) NOT NULL," +
                "gender enum('1','2','3') NOT NULL," +
                "job_number varchar(20) NOT NULL," +
                "foreign key(customer_id) references Users(account) on delete cascade" +
                ");");
        stmt.executeUpdate("create table if not exists Orders(" +
                "order_id int NOT NULL AUTO_INCREMENT primary key," +
                "customer_id varchar(20) NOT NULL," +
                "merchant_id varchar(20) NOT NULL," +
                "order_state int NOT NULL," +
                "foreign key(customer_id) references Customers(customer_id) on delete cascade," +
                "foreign key(merchant_id) references Merchants(merchant_id) on delete cascade," +
                "index idx_order_id (order_id)" +
                ");");
        stmt.executeUpdate("create table if not exists Dishes_Comments(" +
                "customer_id varchar(20) NOT NULL," +
                "dish_id int NOT NULL," +
                "comment varchar(256) NOT NULL," +
                "rating int NOT NULL," +
                "foreign key(customer_id) references Customers(customer_id) on delete cascade," +
                "foreign key(dish_id) references Dishes(dish_id) on delete cascade," +
                "primary key(customer_id, dish_id)" +
                ");");
        stmt.executeUpdate("create table if not exists Merchants_Comments(" +
                "customer_id varchar(20) NOT NULL," +
                "merchant_id varchar(20) NOT NULL," +
                "comment varchar(256) NOT NULL," +
                "rating int NOT NULL," +
                "foreign key(customer_id) references Customers(customer_id) on delete cascade," +
                "foreign key(merchant_id) references Merchants(merchant_id) on delete cascade," +
                "primary key(customer_id, merchant_id)," +
                "CHECK (rating >= 1 AND rating <= 5)" +
                ");");
        stmt.executeUpdate("create table if not exists Favorite_Merchants(" +
                "customer_id varchar(20) NOT NULL," +
                "merchant_id varchar(20) NOT NULL," +
                "foreign key(customer_id) references Customers(customer_id) on delete cascade," +
                "foreign key(merchant_id) references Merchants(merchant_id) on delete cascade," +
                "primary key(customer_id, merchant_id)" +
                ");");
        stmt.executeUpdate("create table if not exists Dishes_Ordered(" +
                "order_id int NOT NULL," +
                "dish_id int NOT NULL," +
                "dish_num int NOT NULL," +
                "foreign key(order_id) references Orders(order_id) on delete cascade," +
                "foreign key(dish_id) references Dishes(dish_id) on delete cascade," +
                "primary key(order_id, dish_id)," +
                "index idx_order_dish (order_id, dish_id)," + // 创建复合索引
                "CHECK (dish_num >= 1 AND dish_num <= " + Constants.MAX_DISH_ORDER_AMOUNT + ")" +
                ");");
        stmt.executeUpdate("create table if not exists Favorite_Dishes(" +
                "customer_id varchar(20) NOT NULL," +
                "dish_id int NOT NULL," +
                "foreign key(customer_id) references Customers(customer_id) on delete cascade," +
                "foreign key(dish_id) references Dishes(dish_id) on delete cascade," +
                "primary key(customer_id, dish_id)" +
                ");");
        stmt.executeUpdate("create or replace view simpleDishView AS " +
                "SELECT dish_id,merchant_id,name,price,type " +
                "FROM Dishes;");

        System.out.println("\n=====数据库初始化完毕=====\n");
    }

    /**
     * 插入数据的方法
     */
    public static void insertData(Connection connection, String tableName, Object[] data) throws SQLException {
        StringBuilder insertQuery = new StringBuilder("INSERT INTO ");
        insertQuery.append(tableName).append(" VALUES (");

        // 构建插入数据的占位符
        for (int i = 0; i < data.length; i++) {
            insertQuery.append("?");
            if (i < data.length - 1) {
                insertQuery.append(",");
            }
        }
        insertQuery.append(")");

        // 使用 PreparedStatement 执行插入操作
        try (PreparedStatement pstmt = connection.prepareStatement(insertQuery.toString())) {
            // 设置插入数据的值
            for (int i = 0; i < data.length; i++) {
                pstmt.setObject(i + 1, data[i]);
            }
            // 执行插入操作
            pstmt.executeUpdate();
        }
    }

    /**
     * 插入新菜品的方法
     */
    public static int insertDish(Connection connection, Object[] data) {
        String query = "INSERT INTO Dishes (merchant_id, name, price, type, description, " +
                "ingredients, possible_allergens, nutrition, rating, image) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            // 开始事务
            connection.setAutoCommit(false);
            PreparedStatement pstmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < data.length; i++) {
                pstmt.setObject(i + 1, data[i]);
            }
            pstmt.executeUpdate();
            ResultSet generatedKeys = pstmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                return generatedKeys.getInt(1);
            } else {
                throw new SQLException("无法获取菜品ID");
            }
        } catch (Exception e) {
            System.out.println("向数据库中添加菜品时出错 " + e.getMessage());
            // 语句执行异常,在catch中进行回滚事务
            try {
                connection.rollback();
            } catch (SQLException ex) {
                System.err.println("事务回滚出错");
            }
        } finally {
            // 在关闭资源之前进行提交,无论是否执行回滚,都需要进行提交,所以将提交放在finally里面
            try {
                connection.commit();
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                System.out.println("添加菜品时事务提交出错");
            }
        }
        return -1;
    }

    public static void deleteData(Connection connection, String tableName, String[] primaryKeyValue,
            String[] primaryKeyColumnName) throws SQLException {
        if (primaryKeyValue.length == 0 || primaryKeyColumnName.length != primaryKeyValue.length) {
            return;
        }
        StringBuilder deleteQuery = new StringBuilder("delete from ");
        deleteQuery.append(tableName).append(" where ").append(primaryKeyColumnName[0]).append(" = ? ");

        // 构建删除数据的占位符
        for (int i = 1; i < primaryKeyColumnName.length; i++) {
            deleteQuery.append("and ").append(primaryKeyColumnName[i]).append(" = ? ");
        }
        deleteQuery.append(";");

        // 使用 PreparedStatement 执行删除操作
        try (PreparedStatement pstmt = connection.prepareStatement(deleteQuery.toString())) {
            // 设置插入数据的值
            for (int i = 0; i < primaryKeyValue.length; i++) {
                pstmt.setObject(i + 1, primaryKeyValue[i]);
            }
            // 执行删除操作
            pstmt.executeUpdate();
        }
    }

    public static ResultSet isItemExists(Connection connection, String tableName, String info, String infoName,
            boolean isInfoClear) throws SQLException {
        String query;
        // 在创建 PreparedStatement 时指定可以滚动的 ResultSet 类型
        PreparedStatement preparedStatement;
        if (isInfoClear) {
            query = "SELECT * FROM " + tableName +
                    " WHERE " + infoName + " = ?";
            preparedStatement = connection.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY);
            preparedStatement.setString(1, info);
        } else {
            query = "SELECT * FROM " + tableName +
                    " WHERE " + infoName + " like '%" + info + "%' ";
            preparedStatement = connection.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY);
        }
        return preparedStatement.executeQuery();
    }

    public static ResultSet isItemExists(Connection connection, String tableName, String mainKey1, String mainKeyName1,
            String mainKey2, String mainKeyName2, boolean isInfoClear) throws SQLException {
        String query;
        PreparedStatement preparedStatement;
        if (isInfoClear) {
            query = "SELECT * FROM " + tableName +
                    " WHERE " + mainKeyName1 + " = ? AND " + mainKeyName2 + " = ? ";

            // 在创建 PreparedStatement 时指定可以滚动的 ResultSet 类型
            preparedStatement = connection.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY);
            preparedStatement.setString(1, mainKey1);
            preparedStatement.setString(2, mainKey2);
        } else {
            query = "SELECT * FROM " + tableName +
                    " WHERE " + mainKeyName1 + " like '%" + mainKey1 + "%' AND " + mainKeyName2 + " = ? ";

            // 在创建 PreparedStatement 时指定可以滚动的 ResultSet 类型
            preparedStatement = connection.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY);
            preparedStatement.setString(1, mainKey2);

        }

        return preparedStatement.executeQuery();
    }

    public static String selectAll(Connection connection, String tableName, String[] columnNames) throws SQLException {
        String query;
        PreparedStatement preparedStatement;
        ResultSet resultSet;
        query = "SELECT * FROM " + tableName;
        preparedStatement = connection.prepareStatement(query,
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        resultSet = preparedStatement.executeQuery();

        // 获取查询结果的元数据
        int columnCount = resultSet.getMetaData().getColumnCount();

        StringBuilder result = new StringBuilder();
        if (getResultSetLength(resultSet) == 0) {
            return "";
        }
        for (int i = 0; i < columnCount; i++) {
            result.append(columnNames[i]).append("\t");
        }
        // 遍历结果集并返回所有列信息
        while (resultSet.next()) {
            result.append("\n");
            for (int i = 1; i <= columnCount; i++) {
                Object columnValue = resultSet.getObject(i);
                result.append(columnValue).append("\t");
            }
        }
        resultSet.close();
        preparedStatement.close();
        return result.toString();
    }

    /**
     * 根据主键从表中获取所有信息的方法
     */
    public static String selectOneRow(Connection connection, String tableName,
            Object primaryKeyValue, String[] columnNames) throws SQLException {
        String query;
        PreparedStatement preparedStatement;
        ResultSet resultSet;
        query = "SELECT COLUMN_NAME " +
                "FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE " +
                " WHERE TABLE_SCHEMA = '" + myConnection.SCHEMA + "' " +
                " AND TABLE_NAME = ? " +
                " AND CONSTRAINT_NAME = 'PRIMARY';";
        preparedStatement = connection.prepareStatement(query);
        preparedStatement.setObject(1, tableName);
        resultSet = preparedStatement.executeQuery();
        String primaryKeyColumn;
        if (resultSet.next()) {
            primaryKeyColumn = resultSet.getString("COLUMN_NAME");
        } else {
            System.out.println("该表不存在主键");
            return "";
        }

        query = "SELECT * FROM " + tableName + " WHERE " + primaryKeyColumn + " = ?";
        preparedStatement = connection.prepareStatement(query);
        // 设置主键值
        preparedStatement.setObject(1, primaryKeyValue);

        // 执行查询
        resultSet = preparedStatement.executeQuery();

        // 获取查询结果的元数据
        int columnCount = resultSet.getMetaData().getColumnCount();

        StringBuilder result = new StringBuilder();
        // 遍历结果集并返回所有列信息
        if (resultSet.next()) {
            for (int i = 1; i <= columnCount; i++) {
                Object columnValue = resultSet.getObject(i);
                result.append(columnNames[i - 1]).append("\t: ").append(columnValue).append("\n");
            }
        }
        return result.toString();
    }

    public static String getNameById(Connection connection, String tableName, String idColumnName,
            String nameColumnName, String id) throws SQLException {
        String query = " SELECT " + nameColumnName +
                " FROM " + tableName +
                " WHERE " + idColumnName + " = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setString(1, id);
        ResultSet resultSet = preparedStatement.executeQuery();
        if (resultSet.next()) {
            return resultSet.getString(nameColumnName);
        } else {
            System.out.println("没有与ID匹配的查询结果");
            return null;
        }
    }

    public static ResultSet showAllRelatedOrders(Connection connection, String accountId, String idColumnName,
            boolean checkUnCommented) {
        ResultSet orderResultSet = null;
        try {
            String selectOrderSql = "SELECT * FROM Orders WHERE " + idColumnName + " = ?";
            if (checkUnCommented) {
                selectOrderSql += (" and (order_state = " + Constants.FINISHED
                        + " or order_state = " + Constants.INFORMED + ")");
            }
            selectOrderSql += ";";
            PreparedStatement pstmt = connection.prepareStatement(selectOrderSql,
                    ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            pstmt.setString(1, accountId);
            orderResultSet = pstmt.executeQuery();
            showSingleOrder(connection, orderResultSet);
        } catch (SQLException e) {
            // System.out.println(e.getMessage());
            System.err.println("订单显示出错");
        }
        return orderResultSet;
    }

    public static ResultSet getUnfinishedOrders(Connection connection, String accountId, String idColumnName) {
        try {
            String selectOrderSql = "SELECT * FROM Orders WHERE " + idColumnName + " = ? AND order_state = ?;";
            // 可更新的resultSet
            PreparedStatement preparedStatement = connection.prepareStatement(selectOrderSql,
                    ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            preparedStatement.setString(1, accountId);
            preparedStatement.setInt(2, Constants.UNFINISHED);
            return preparedStatement.executeQuery();
        } catch (SQLException e) {
            System.err.println("从数据库中获取未完成订单时出错");
            return null;
        }
    }

    public static void showSingleOrder(Connection connection, ResultSet orderResultSet) {
        try {
            ResultSet dishResultSet = null;
            int count = 0;
            // 遍历所有订单
            orderResultSet.afterLast();
            while (orderResultSet.previous()) {
                count++;
                int orderId = orderResultSet.getInt("order_id");
                int orderState = orderResultSet.getInt("order_state");
                String merchantId = orderResultSet.getString("merchant_id");

                String selectDishesOrderedSql = "SELECT * FROM Dishes_Ordered WHERE order_id = ?;";
                PreparedStatement pstmt = connection.prepareStatement(selectDishesOrderedSql,
                        ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                pstmt.setInt(1, orderId);
                dishResultSet = pstmt.executeQuery();
                String merchantName = getNameById(connection, "Merchants",
                        "merchant_id", "name", merchantId);
                System.out.print(count + "\t订单号：" + orderId + "\t订单状态：" +
                        Translator.translateOrderState(orderState) + "\t商家名称：" + merchantName
                        + "\t菜品： ");
                // 显示该订单各个菜品有几份
                while (dishResultSet.next()) {
                    String dishName = getNameById(connection, "Dishes",
                            "dish_id", "name", dishResultSet.getString("dish_id"));
                    System.out.print(dishName + "  " +
                            dishResultSet.getInt("dish_num") + "份  ");
                }
                System.out.println();
            }
            if (count == 0) {
                System.out.println("当前没有相关订单");
            }
        } catch (SQLException e) {
            // System.out.println(e.getMessage());
            System.err.println("展示订单详细信息时出错");
        } catch (OrderStateUndefinedExpection e) {
            System.err.println(e.getMessage());
        }
    }

    public static ResultSet showDishesInOrder(Connection connection, ResultSet orderResultSet) {
        ResultSet dishResultSet = null;
        try {
            int count = 0;
            int orderId = orderResultSet.getInt("order_id");

            String selectDishesOrderedSql = "SELECT * FROM Dishes_Ordered WHERE order_id = ?;";
            PreparedStatement pstmt = connection.prepareStatement(selectDishesOrderedSql,
                    ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            pstmt.setInt(1, orderId);
            dishResultSet = pstmt.executeQuery();
            System.out.println("菜品：");
            // 显示该订单各个菜品有几份
            while (dishResultSet.next()) {
                count++;
                String dishName = getNameById(connection, "Dishes",
                        "dish_id", "name", dishResultSet.getString("dish_id"));
                System.out.println(count + "\t" + dishName + "\t" +
                        dishResultSet.getInt("dish_num") + "份  ");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            System.err.println("展示订单详细信息时出错");
        }
        return dishResultSet;

    }

    public static void showAllComments(Connection connection, Scanner scanner, String tableName, Object id,
            String idColumnName) {
        String selectMerchantCommentSql = "SELECT * FROM " + tableName + " WHERE " + idColumnName + " = ?;";
        try (PreparedStatement pstmt = connection.prepareStatement(selectMerchantCommentSql,
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
            pstmt.setObject(1, id);
            ResultSet commentResultSet = pstmt.executeQuery();
            if (getResultSetLength(commentResultSet) == 0) {
                System.out.println("还没有人评论");
                return;
            }
            int count = 0;
            while (commentResultSet.next()) {
                count++;
                String customerName = getNameById(connection, "Customers",
                        "customer_id", "name", commentResultSet.getString("customer_id"));
                System.out.println(count + "\t评论人：" + customerName + "\t评论内容：" + commentResultSet.getString("comment"));
                if (count % Constants.MAX_DISPLAY_NUM == 0) {
                    if (!User_interface.yesOrNo(scanner, "列表过长，是否继续显示（Y/N）？")) {
                        System.out.println("显示已中断");
                        break;
                    }
                }
            }
            commentResultSet.close();
        } catch (SQLException e) {
            // System.out.println(e.getMessage());
            System.out.println("评论显示出错");
        }
    }

    public static void showDishDetail(Connection connection, int dishId) {
        String selectDishSql = "SELECT * FROM dishes WHERE dish_id = ?;";
        try (PreparedStatement pstmt = connection.prepareStatement(selectDishSql)) {
            pstmt.setInt(1, dishId);
            ResultSet dishResultSet = pstmt.executeQuery();

            if (dishResultSet.next()) {
                System.out.print("菜品名称:\t");
                System.out.println(dishResultSet.getString("name"));
                System.out.print("菜品价格:\t");
                System.out.println(dishResultSet.getFloat("price"));
                System.out.print("菜品分类:\t");
                System.out.println(dishResultSet.getString("type"));
                System.out.print("菜品描述:\t");
                System.out.println(dishResultSet.getString("description"));
                System.out.print("菜品原料:\t");
                System.out.println(dishResultSet.getString("ingredients"));
                System.out.print("可能的过敏源:\t");
                System.out.println(dishResultSet.getString("possible_allergens"));
                System.out.print("营养成分:\t");
                System.out.println(dishResultSet.getString("nutrition"));
                System.out.print("菜品评分:\t");
                System.out.println(dishResultSet.getString("rating"));
                System.out.print("菜品图片:\t");
                System.out.println(dishResultSet.getString("image"));
            }
            dishResultSet.close();
        } catch (SQLException e) {
            // System.out.println(e.getMessage());
            System.out.println("菜品查询出错");
        }
    }

    public static void orderStateChanged(Connection connection, int orderId, int newOrderState) {
        String query = "SELECT * FROM Orders WHERE order_id = ?;";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_UPDATABLE);) {
            preparedStatement.setInt(1, orderId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                int oldOrderState = resultSet.getInt("order_state");
                if (oldOrderState + 1 != newOrderState) {
                    throw new IllegalOrderStateChangeException(oldOrderState, newOrderState);
                }
                resultSet.updateInt("order_state", newOrderState);
                resultSet.updateRow();
            } else {
                throw new OrderNotFoundException(orderId);
            }
            resultSet.close();
        } catch (SQLException e) {
            System.err.println("改变订单" + orderId + "状态时出错");
        } catch (OrderNotFoundException | IllegalOrderStateChangeException e) {
            System.out.println(e.getMessage());
        }
    }

    public static int getResultSetLength(ResultSet resultSet) {
        try {
            resultSet.last();// 移到最后一行
            int rows = resultSet.getRow();
            resultSet.beforeFirst();// 移到初始位置
            return rows;
        } catch (SQLException e) {
            System.out.println("获取结果集长度时出错");
        }
        return -1;
    }

    public static void update(Connection connection, String tableName, Object primaryKeyValue,
            String primaryKeyColumnName, String[] updateColumnNames, Object[] updateTargets) throws SQLException {
        if (updateColumnNames.length == 0 || updateColumnNames.length != updateTargets.length) {
            return;
        }
        StringBuilder query = new StringBuilder("update " + tableName +
                " set " + updateColumnNames[0] + " = ?");
        for (int i = 1; i < updateColumnNames.length; i++) {
            query.append(", ").append(updateColumnNames[i]).append(" = ?");
        }
        query.append(" where ").append(primaryKeyColumnName).append(" = ?;");
        PreparedStatement pstmt = connection.prepareStatement(query.toString());
        int i;
        for (i = 0; i < updateColumnNames.length; i++) {
            pstmt.setObject(i + 1, updateTargets[i]);
        }
        pstmt.setObject(i + 1, primaryKeyValue);
        pstmt.executeUpdate();
        pstmt.close();
    }

    public static float getNewRating(Connection connection, String tableName,
            String primaryKeyColumn, Object primaryKeyValue) {
        float result = 0;
        try {
            String query = "SELECT * FROM " + tableName + " WHERE " + primaryKeyColumn + " = ?";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setObject(1, primaryKeyValue);

            ResultSet resultSet = preparedStatement.executeQuery();

            int count = 0;
            int ratingAmount = 0;
            while (resultSet.next()) {
                ratingAmount += resultSet.getInt("rating");
                count++;
            }
            result = (float) ratingAmount / (float) count;
        } catch (SQLException e) {
            System.out.println("查询评分出错");
        }
        return result;
    }

    public static String[] getAllMerchantId(Connection connection) throws SQLException {
        Statement stmt = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        ResultSet merchantResultSet = stmt.executeQuery("select * from Merchants");
        String[] result = new String[getResultSetLength(merchantResultSet)];
        int count = 0;
        while (merchantResultSet.next()) {
            result[count] = merchantResultSet.getString("merchant_id");
            count++;
        }
        return result;
    }

    public static int getFavoriteNum(Connection connection, String merchantId) throws SQLException {
        PreparedStatement pstmt = connection.prepareStatement("select * from favorite_merchants where merchant_id = ?",
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        pstmt.setString(1, merchantId);
        ResultSet rs = pstmt.executeQuery();
        return getResultSetLength(rs);
    }

    public static int getTotalDishSales(Connection connection, int dishId) {
        String query = "SELECT SUM(dish_num) AS total_sales FROM Dishes_Ordered WHERE dish_id = ?";
        int totalSales = 0;

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, dishId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    totalSales = rs.getInt("total_sales");
                }
            }
        } catch (SQLException e) {
            System.err.println("获取菜品总销量时出错");
        }
        return totalSales;
    }

    public static ResultSet getSpecialtyDishesByMerchantId(Connection connection, String merchantId) {
        ResultSet resultSet = null;
        String query = "SELECT * FROM Dishes WHERE merchant_id = ? AND is_specialty = TRUE";

        try {
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setString(1, merchantId);
            resultSet = pstmt.executeQuery();
        } catch (SQLException e) {
            System.err.println("获取特色菜品时出错");
        }
        return resultSet;
    }
}
