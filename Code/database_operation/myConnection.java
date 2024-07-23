package database_operation;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class myConnection {
    // 声明Connection对象
    public static Connection con;
    private static final String URL = "jdbc:mysql://localhost:3306/dbd_pj";
    private static final String USER = "root";
    private static final String PASSWORD = "BeeCool";
    public static final String SCHEMA = "dbd_pj";

    /** 建立返回值为Connection的方法 */
    public static Connection getConnection() {
        try { // 加载数据库驱动类
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("数据库驱动加载成功");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        try { // 通过访问数据库的URL获取数据库连接对象
            con = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("数据库连接成功");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        // 按方法要求返回一个Connection对象
        return con;
    }
}