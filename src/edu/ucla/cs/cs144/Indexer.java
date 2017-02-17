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

public class Indexer {

    /** Creates a new instance of Indexer */
    public Indexer() {
    }

    private IndexWriter indexWriter = null;

    public IndexWriter getIndexWriter(boolean create) throws IOException {
        if (indexWriter == null) {
            Directory indexDir = FSDirectory.open(new File("var/lib/lucene/"));
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

    public void indexItem(Item item) throws IOException {
        //System.out.println("Indexing item: " + item);
        IndexWriter writer = getIndexWriter(false);
        Document doc = new Document();
        doc.add(new StringField("id", item.getId(), Field.Store.YES));
        doc.add(new StringField("name", item.getName(), Field.Store.YES));
        doc.add(new TextField("description", item.getDescription(), Field.Store.YES));
        if(item.getDescription() == null) {
          System.out.println("Error, description is null!!!!!!!!!!!");
        }
        String fullSearchableText = item.getName() + " " + item.getDescription();
        doc.add(new TextField("content", fullSearchableText, Field.Store.NO));
        writer.addDocument(doc);
    }

    public void indexCategory(Category category) throws IOException {
        //System.out.println("Indexing category: " + category);
        IndexWriter writer = getIndexWriter(false);
        Document doc = new Document();
        doc.add(new StringField("itemId", category.getItemId(), Field.Store.YES));
        doc.add(new StringField("name", category.getName(), Field.Store.YES));
        String fullSearchableText = category.getName();
        doc.add(new TextField("content", fullSearchableText, Field.Store.NO));
        writer.addDocument(doc);
    }

    public void rebuildIndexes() throws IOException{

        Connection conn = null;
        // create a connection to the database to retrieve Items from MySQL
        try {
            conn = DbManager.getConnection(true);
        } catch (SQLException ex) {
            System.out.println(ex);
        }


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

        getIndexWriter(true);
        // Index all item entries
        ArrayList<Item> items = DbManager.getItems(); // this is supposed to be done in JDBC
        for (Item item : items) {
            indexItem(item);
        }
        // Index all category entries
        ArrayList<Category> categories = DbManager.getCategories();
        for (Category category : categories) {
            indexCategory(category);
        }

            // close the database connection
        try {
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
