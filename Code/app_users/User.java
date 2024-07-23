package app_users;

import app_operation.SystemState;

import java.sql.*;
import java.util.*;

public abstract class User {
    Connection connection;
    Scanner scanner;
    String accountId;

    public User(Connection connection, Scanner scanner, String accountId) {
        this.connection = connection;
        this.scanner = scanner;
        this.accountId = accountId;
    }


    public abstract void forceInfoSet() throws SQLException;

    public abstract void printHelpMessage();

    public void exitSystem(){
        System.out.println("感谢使用本程序！");
        SystemState.isUserLogin=false;
        SystemState.isExit=true;
    }

    public abstract void getInstructions() throws SQLException;

}
