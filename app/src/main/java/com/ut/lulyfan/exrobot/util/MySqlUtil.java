package com.ut.lulyfan.exrobot.util;

import com.ut.lulyfan.exrobot.model.Record;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Created by Administrator on 2017/12/7/007.
 */

public class MySqlUtil {

    private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    private static final String DB_URL = "jdbc:mysql://59.110.242.105:3306/ex";
    private static final String USER = "lulyfan";
    private static final String PASS = "lan414337795";

    public static Connection openDB() throws SQLException {

        try {
            Class.forName(JDBC_DRIVER);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println("start connecting db...");
        Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
        System.out.println(connection == null ? "open db failed" : "open db success");
        return connection;
    }

    public static void insertData(Record record) throws SQLException {
        Connection connection = openDB();
        insertData(connection, record.getSn(), record.getTask(), record.getDescription(), record.getLocation(), record.getTime());
        connection.close();
    }

    public static void insertData(List<Record> records) throws SQLException {
        Connection connection = openDB();
        for (Record record : records) {
            insertData(connection, record.getSn(), record.getTask(), record.getDescription(), record.getLocation(), record.getTime());
            records.remove(record);
        }
        connection.close();
    }

    private static void insertData(Connection connection, String sn, String task, String description, String location, String time) throws SQLException {

        Statement statement = null;
        try {
            statement = connection.createStatement();
            String sn2 = "'" + sn + "',";
            String task2 = "'" + task + "',";
            String description2 = "'" + description + "',";
            String location2 = "'" + location + "',";
            String time2 = "'" + time + "'";
            String sql = "INSERT INTO data (sn, task, description, location, start_time) VALUES (" + sn2 + task2 + description2 +  location2 + time2 +")";
            statement.executeUpdate(sql);
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }
}
