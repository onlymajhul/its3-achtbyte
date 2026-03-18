package de.tls.discord.db;

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

            String[] columns = {
                "job VARCHAR DEFAULT 'Arbeitslos'",
                "job_level INT DEFAULT 0",
                "education_type VARCHAR DEFAULT 'None'",
                "education_end BIGINT DEFAULT 0",
                "last_work BIGINT DEFAULT 0",
                "last_crime BIGINT DEFAULT 0",
                "criminal_records INT DEFAULT 0",
                "has_weapon BOOLEAN DEFAULT FALSE"
            };

            for (String col : columns) {
                try {
                    stmt.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS " + col);
                } catch (Exception ignored) {}
            }

            System.out.println("H2 Database connected.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() {
        return connection;
    }
}