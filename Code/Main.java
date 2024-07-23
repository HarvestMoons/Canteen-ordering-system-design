import app_users.User;
import database_operation.*;

import java.sql.*;
import java.util.*;
import app_operation.*;

public class Main {

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            Connection connection = myConnection.getConnection();
            // Table_handler.dropAllTables(connection);
            Table_handler.databaseInitialize(connection);
            while (!SystemState.isExit) {
                User user = User_interface.userLogin(scanner, connection);
                assert user != null;
                user.forceInfoSet();
                user.getInstructions();
            }
            connection.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage() + "SQL Fail");
        }
    }
}
