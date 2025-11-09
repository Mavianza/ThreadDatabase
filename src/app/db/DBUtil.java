package app.db;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBUtil {
    private static String URL;
    private static String USER;
    private static String PASS;

    static {
        try {
            // load db.properties dari classpath: /app/db/db.properties
            Properties p = new Properties();
            try (InputStream in = DBUtil.class.getResourceAsStream("/app/db/db.properties")) {
                if (in == null) throw new RuntimeException("db.properties tidak ditemukan di classpath");
                p.load(in);
            }
            URL  = p.getProperty("db.url");
            USER = p.getProperty("db.user");
            PASS = p.getProperty("db.password");

            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (Exception e) {
            throw new RuntimeException("Gagal inisialisasi DBUtil: " + e.getMessage(), e);
        }
    }

    public static Connection get() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
