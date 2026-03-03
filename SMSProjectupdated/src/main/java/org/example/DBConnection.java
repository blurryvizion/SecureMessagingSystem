package org.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;
import java.io.InputStream;

public class DBConnection {

    public static Connection getConnection() throws Exception {

        Properties props = new Properties();
        InputStream input = DBConnection.class
                .getClassLoader()
                .getResourceAsStream("db.properties");

        if (input == null) {
            throw new RuntimeException("db.properties file not found");
        }

        props.load(input);

        return DriverManager.getConnection(
                props.getProperty("db.url"),
                props.getProperty("db.user"),
                props.getProperty("db.password")
        );
    }
}
