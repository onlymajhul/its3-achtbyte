package de.tle.discord.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class Database {

    private static Connection connection;

    public static void connect() {
        try {
            connection = DriverManager.getConnection("jdbc:h2:./data/database");

            Statement stmt = connection.createStatement();

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id BIGINT PRIMARY KEY,
                    cash INT DEFAULT 1000,
                    bank INT DEFAULT 0,
                    wins INT DEFAULT 0,
                    losses INT DEFAULT 0
                )
            """);

            System.out.println("H2 Database connected.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() {
        return connection;
    }
}