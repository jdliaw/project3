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

            // moved from DbManager
            // Execute select statement for Category

            // What I did here was execute the query, and look for the stuff that we want.
            // Rather than using IndexWriter to write index once for category, and once for Item, that was combined...
            // So first, execute query and go through the results.
            // If an itemID doesn't exist yet, add it with its corresponding category.
            // If an itemID does exist, just append that category in the HashMap.
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
            // Now we go through the Item query results. 
            // I just used code to get the name, description, and itemID.
            // Math the itemID to the category_itemId and if that's there, then throw it all together into the fullSearchableText variable.
            // Add that fullSearchableText to doc, and then finally write only once (Category appended into the Item)
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

