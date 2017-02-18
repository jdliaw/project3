package edu.ucla.cs.cs144;

import java.io.IOException;
import java.io.StringReader;
import java.io.File;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.util.ArrayList;
import java.util.HashMap;

public class Indexer {

    /** Creates a new instance of Indexer */
    public Indexer() {
    }

    private IndexWriter indexWriter = null;

    public IndexWriter getIndexWriter(boolean create) throws IOException {
        if (indexWriter == null) {
            Directory indexDir = FSDirectory.open(new File("var/lib/lucene/index1"));
            IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_10_2, new StandardAnalyzer());
            indexWriter = new IndexWriter(indexDir, config);
        }
        return indexWriter;
    }

    public void closeIndexWriter() throws IOException {
        if (indexWriter != null) {
            indexWriter.close();
        }
    }

    // public void indexItem(Item item) throws IOException {
    //     //System.out.println("Indexing item: " + item);
    //     IndexWriter writer = getIndexWriter(false);
    //     Document doc = new Document();
    //     doc.add(new StringField("id", item.getId(), Field.Store.YES));
    //     doc.add(new StringField("name", item.getName(), Field.Store.YES));
    //     doc.add(new TextField("description", item.getDescription(), Field.Store.YES));
    //     if(item.getDescription() == null) {
    //       System.out.println("Error, description is null!!!!!!!!!!!");
    //     }
    //     String fullSearchableText = item.getName() + " " + item.getDescription();
    //     doc.add(new TextField("content", fullSearchableText, Field.Store.NO));
    //     writer.addDocument(doc);
    // }

    // public void indexCategory(Category category) throws IOException {
    //     //System.out.println("Indexing category: " + category);
    //     IndexWriter writer = getIndexWriter(false);
    //     Document doc = new Document();
    //     doc.add(new StringField("itemId", category.getItemId(), Field.Store.YES));
    //     doc.add(new StringField("name", category.getName(), Field.Store.YES));
    //     String fullSearchableText = category.getName();
    //     doc.add(new TextField("content", fullSearchableText, Field.Store.NO));
    //     writer.addDocument(doc);
    // }

    public void rebuildIndexes() throws IOException{
        IndexWriter writer = getIndexWriter(false);

        Connection conn = null;
        // create a connection to the database to retrieve Items from MySQL
        try {
            conn = DbManager.getConnection(true);
            String category_itemId;
            String category_name;
            String item_name;
            String item_id;
            String description;
            String fullSearchableText;
            HashMap categories = new HashMap();

            Statement s = conn.createStatement();

            //moved from DbManager
            // Execute select statement for Category
            ResultSet cat_rs = s.executeQuery("SELECT * FROM Category");
            while (cat_rs.next()) {
                category_name = cat_rs.getString("Category");
                category_itemId = cat_rs.getString("ItemId");
                //check to see if item id exists in HashMap, if not, create it and place category in
                if(!categories.containsKey(category_itemId)) {
                    categories.put(category_itemId, category_name);
                }
                //else, item id is already there so we just append.
                else {
                    String append_category = categories.get(category_itemId) + " " + category_name;
                    categories.put(category_itemId, append_category);
                }
            }


            //moved from DbManager
                        // Execute select statement for Items
            ResultSet items_rs = s.executeQuery("SELECT * FROM Item");
            while (items_rs.next()) {
                item_name = items_rs.getString("Name");
                description = items_rs.getString("Description");
                item_id = items_rs.getString("ItemId");

                //Storing stuff
                Document doc = new Document();
                doc.add(new StringField("id", item_id, Field.Store.YES));
                doc.add(new StringField("name", item_name, Field.Store.YES));
                fullSearchableText = item_name + " " + categories.get(item_id) + " "  + description;
                doc.add(new TextField("content", fullSearchableText, Field.Store.NO));
                writer.addDocument(doc);
            }

            //close everything
            s.close();
            cat_rs.close();
            items_rs.close();
            conn.close();

        } catch (SQLException ex) {
            System.out.println(ex);
        }
        closeIndexWriter();


        /*
         * Add your code here to retrieve Items using the connection`
         * and add corresponding entries to your Lucene inverted indexes.
             *
             * You will have to use JDBC API to retrieve MySQL data from Java.
             * Read our tutorial on JDBC if you do not know how to use JDBC.
             *
             * You will also have to use Lucene IndexWriter and Document
             * classes to create an index and populate it with Items data.
             * Read our tutorial on Lucene as well if you don't know how.
             *
             * As part of this development, you may want to add
             * new methods and create additional Java classes.
             * If you create new classes, make sure that
             * the classes become part of "edu.ucla.cs.cs144" package
             * and place your class source files at src/edu/ucla/cs/cs144/.
         *
         */

        // getIndexWriter(true);
        // // Index all item entries
        // ArrayList<Item> items = DbManager.getItems(); // this is supposed to be done in JDBC
        // for (Item item : items) {
        //     indexItem(item);
        // }
        // // Index all category entries
        // ArrayList<Category> categories = DbManager.getCategories();
        // for (Category category : categories) {
        //     indexCategory(category);
        // }

        //     // close the database connection
        // try {
        //     conn.close();
        // } catch (SQLException ex) {
        //     System.out.println(ex);
        // }
        // closeIndexWriter();
    }

    public static void main(String args[]) {
        Indexer idx = new Indexer();
        try {
            idx.rebuildIndexes();
        }
        catch(IOException e) {
            System.err.println("Rebuild Indexes IOException");
        }
    }
}





// package edu.ucla.cs.cs144;

// import java.util.ArrayList;
// import java.sql.Statement;
// import java.sql.ResultSet;
// import java.sql.Connection;
// import java.sql.DriverManager;
// import java.sql.SQLException;

// public class DbManager {
//         static private String databaseURL = "jdbc:mysql://localhost:3306/";
//         static private String dbname = "CS144";
//         static private String username = "cs144";
//         static private String password = "";

//         static private String item_id, item_name, description;
//         static private String category_itemId, category_name;

//         static private ArrayList<Item> items = new ArrayList<Item>();
//         static private ArrayList<Category> categories = new ArrayList<Category>();

// 	/**
// 	 * Opens a database connection
// 	 * @param dbName The database name
// 	 * @param readOnly True if the connection should be opened read-only
// 	 * @return An open java.sql.Connection
// 	 * @throws SQLException
// 	 */
// 	public static Connection getConnection(boolean readOnly)
// 	throws SQLException {
//             Connection conn = DriverManager.getConnection(
//                 databaseURL + dbname, username, password);
//             conn.setReadOnly(readOnly);
//             return conn;
//         }

// 	private DbManager() {}

//   public static ArrayList<Item> getItems() {
//     // for (int i = 0; i < items.size(); i++) {
//     //   System.out.println(items.get(i).getName());
//     // }
//     return items;
//   }

//   public static ArrayList<Category> getCategories() {
//     // for (int i = 0; i < categories.size(); i++) {
//     //   System.out.println(categories.get(i).getName());
//     // }
//     return categories;
//   }

// 	static {
// 		try {
// 			Class.forName("com.mysql.jdbc.Driver").newInstance();

//       Connection c = getConnection(true);

//       Statement s = c.createStatement();

//       // Execute select statement for Items
//       ResultSet items_rs = s.executeQuery("SELECT Name, Description, ItemId FROM Item");
//       while (items_rs.next()) {
//         item_name = items_rs.getString("Name");
//         description = items_rs.getString("Description");
//         if(description == null) {
//           System.out.println("Error, description is null");
//         }
//         item_id = items_rs.getString("ItemId");

//         Item item = new Item(item_id, item_name, description);
//         items.add(item);
//         // System.out.println("Item " + item_id + ": " + item_name + " (" + description + ")");
//       }

//       // Execute select statement for Category
//       ResultSet cat_rs = s.executeQuery("SELECT Category, ItemId FROM Category");
//       while (cat_rs.next()) {
//         category_name = cat_rs.getString("Category");
//         category_itemId = cat_rs.getString("ItemId");

//         Category category = new Category(category_itemId, category_name);
//         // System.out.println(category.getName() + " /// " + category.getItemId());
//         categories.add(category);
//         // System.out.println("Category " + category + " - " + category_itemId);
//       }

//       items_rs.close();
//       cat_rs.close();
//       s.close();
//       c.close();
//       // getItems();
//       // getCategories();

// 		} catch(Exception e) {
// 			e.printStackTrace();
// 			throw new RuntimeException(e);
// 		}
// 	}
// }
