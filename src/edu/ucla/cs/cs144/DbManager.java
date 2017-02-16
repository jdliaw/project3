package edu.ucla.cs.cs144;

import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbManager {
        static private String databaseURL = "jdbc:mysql://localhost:3306/";
        static private String dbname = "CS144";
        static private String username = "cs144";
        static private String password = "";

        static private String item_name, description;
        static private int item_id;
        static private String category;
        static private int category_itemId;

	/**
	 * Opens a database connection
	 * @param dbName The database name
	 * @param readOnly True if the connection should be opened read-only
	 * @return An open java.sql.Connection
	 * @throws SQLException
	 */
	public static Connection getConnection(boolean readOnly)
	throws SQLException {
            Connection conn = DriverManager.getConnection(
                databaseURL + dbname, username, password);
            conn.setReadOnly(readOnly);
            return conn;
        }

	private DbManager() {}

	static {
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();

      Connection c = getConnection(true);

      Statement s = c.createStatement();

      // Execute select statement for Items
      ResultSet items_rs = s.executeQuery("SELECT * FROM Item");
      while (items_rs.next()) {
        item_name = items_rs.getString("Name");
        description = items_rs.getString("Description");
        item_id = items_rs.getInt("ItemId");
        // System.out.println("Item " + item_id + ": " + item_name + " (" + description + ")");
      }

      // Execute select statement for Category
      ResultSet cat_rs = s.executeQuery("SELECT * FROM Category");
      while (cat_rs.next()) {
        category = cat_rs.getString("Category");
        category_itemId = cat_rs.getInt("ItemId");
        // System.out.println("Category " + category + " - " + category_itemId);
      }

      items_rs.close();
      s.close();
      c.close();

		} catch(Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}
