package edu.ucla.cs.cs144;

import java.util.ArrayList;
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

        static private String item_id, item_name, description;
        static private String category_itemId, category_name;

        static private ArrayList<Item> items = new ArrayList<Item>();
        static private ArrayList<Category> categories = new ArrayList<Category>();

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

  public static ArrayList<Item> getItems() {
    // for (int i = 0; i < items.size(); i++) {
    //   System.out.println(items.get(i).getName());
    // }
    return items;
  }

  public static ArrayList<Category> getCategories() {
    // for (int i = 0; i < categories.size(); i++) {
    //   System.out.println(categories.get(i).getName());
    // }
    return categories;
  }

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
        item_id = items_rs.getString("ItemId");

        Item item = new Item(item_id, item_name, description);
        items.add(item);
        // System.out.println("Item " + item_id + ": " + item_name + " (" + description + ")");
      }

      // Execute select statement for Category
      ResultSet cat_rs = s.executeQuery("SELECT * FROM Category");
      while (cat_rs.next()) {
        category_name = cat_rs.getString("Category");
        category_itemId = cat_rs.getString("ItemId");

        Category category = new Category(category_itemId, category_name);
        // System.out.println(category.getName() + " /// " + category.getItemId());
        categories.add(category);
        // System.out.println("Category " + category + " - " + category_itemId);
      }

      items_rs.close();
      cat_rs.close();
      s.close();
      c.close();
      // getItems();
      // getCategories();

		} catch(Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}
