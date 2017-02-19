package edu.ucla.cs.cs144;

import java.util.HashSet;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import java.util.Date;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;

import org.w3c.dom.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import java.io.StringWriter;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.lucene.document.Document;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import edu.ucla.cs.cs144.DbManager;
import edu.ucla.cs.cs144.SearchRegion;
import edu.ucla.cs.cs144.SearchResult;
import java.sql.PreparedStatement;

public class AuctionSearch implements IAuctionSearch {

	/*
         * You will probably have to use JDBC to access MySQL data
         * Lucene IndexSearcher class to lookup Lucene index.
         * Read the corresponding tutorial to learn about how to use these.
         *
	 * You may create helper functions or classes to simplify writing these
	 * methods. Make sure that your helper functions are not public,
         * so that they are not exposed to outside of this class.
         *
         * Any new classes that you create should be part of
         * edu.ucla.cs.cs144 package and their source files should be
         * placed at src/edu/ucla/cs/cs144.
         *
         */

	//perform a basic search
	public SearchResult[] basicSearch(String query, int numResultsToSkip,
			int numResultsToReturn) {

			// Instantiate stuff and perform the search. Store this in "TopDocs" which is the top docs of the number of stuff we queried
			SearchEngine se = null;
			TopDocs topDocs = null;
			int numTotalQueries = numResultsToSkip + numResultsToReturn;							//we need to query this many, and only take the bottom numResultsToReturn
			try {
				se = new SearchEngine();
				// System.out.println("numTotalQueries: " + numTotalQueries);
				// if (numTotalQueries == 0) {

				// }
				topDocs = se.performSearch(query, numTotalQueries); 										//search
			} catch (Exception e) {
				System.out.println("error");
			}

			// obtain the ScoreDoc (= documentID, relevanceScore) array from topDocs
			ScoreDoc[] hits = topDocs.scoreDocs;

			// At this point, we have performed our search and gotten the top docs that we want. This
			// is scored and then stored in a variable called hits.

			// Now, we need to determine the size of our result size. This can be done with a little bit of math due to
			// hits.length, numResultsToSkip, and numResultsToReturn.
			int maxResultSize;

			//if we're skipping more than the docs we have, we skip them all, so 0
			if(hits.length - numResultsToSkip < 0) {
				maxResultSize = 0;
			}
			//return a max of how many hits we got minus the number we skip
			else if(hits.length - numResultsToSkip < numResultsToReturn) {
				maxResultSize = hits.length - numResultsToSkip;
			}
			//or, we can only return a max of the number of results specified to return
			else {
				maxResultSize = numResultsToReturn;
			}
			//Now, create our array
			SearchResult[] result = new SearchResult[maxResultSize];

			// Now, retrive the matching document from our ScoreDoc array. We start at numResultsToSkip and finish at our maxResultSize + numResultsToSkip
			// for a total of maxResultSize retrievals.
			for (int i = numResultsToSkip; i < maxResultSize + numResultsToSkip; i++) {
					Document doc =  null;
					try {
						doc = se.getDocument(hits[i].doc);
					}catch(IOException e) {
						System.err.println("IOException");
					}
					String description = doc.get("description");
					String name = doc.get("name");
					String id = doc.get("id");

					int adjustedResultPos = i - numResultsToSkip;
					result[adjustedResultPos] = new SearchResult(id, name);

			}
			return result;
	}

	public SearchResult[] spatialSearch(String query, SearchRegion region,
			int numResultsToSkip, int numResultsToReturn) throws IOException, ParseException {

		HashSet<Integer> spatial_rs = new HashSet<Integer>();

    try {
      Connection conn = DbManager.getConnection(true);

			//check the mysql docs here: https://dev.mysql.com/doc/refman/5.7/en/using-spatial-indexes.html to find out more
			String geom = "GeomFromText('Polygon((" + region.getLx() + " " + region.getLy() + ", " + region.getLx() + " " + region.getRy() + ", " + region.getRx() + " " + region.getRy() + ", " + region.getRx() + " " + region.getLy() + ", " + region.getLx() + " " + region.getLy() +  "))')";

			//See the above comment...
      Statement s = conn.createStatement();
      ResultSet rs = s.executeQuery("SELECT ItemID FROM Location WHERE MBRContains("
          + geom + ",Position);");

			//loop through results and add our item ids
      while (rs.next()) {
          spatial_rs.add(rs.getInt("ItemID"));
      }

      // close connections
      s.close();
      rs.close();
      conn.close();

		}
		catch (SQLException e){
      System.out.println(e);
    }

    // System.out.println("spatial size: " + spatialResultSet.size());
    SearchResult[] basic_search = basicSearch(query, 0, spatial_rs.size());
    ArrayList<SearchResult> results = new ArrayList<SearchResult>();

		int skippedResults = 0;
    for (SearchResult r: basic_search) {
    	if (numResultsToReturn < 0) {
					break;
			}
			if(spatial_rs.contains(Integer.parseInt(r.getItemId()))) {
				if (skippedResults < numResultsToSkip) {
					skippedResults++;
					continue;
				}
				results.add(r);
			}
    }
		return results.toArray(new SearchResult[results.size()]);
	}

	public String sqlToXmlTime(String sqlTime) {
		SimpleDateFormat xmlFormat = new SimpleDateFormat("MMM-dd-yy HH:mm:ss");
    SimpleDateFormat sqlFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Date date = null;

    try {
        date = sqlFormat.parse(sqlTime);
    }
    catch(Exception e) {
        System.out.println("ERROR: Cannot parse \"" + sqlTime + "\"");
    }

    return xmlFormat.format(date);
 	}

	public String getXMLDataForItemId(String itemId) {
		String xmlstore = "";
    Connection dbconn = null;
    // Create a connection to the database
    try
    {
      // Connection to db manager
      dbconn = DbManager.getConnection(true);
      Statement s = dbconn.createStatement();
      ResultSet rs = s.executeQuery("SELECT * FROM Item " + "WHERE ItemID = " + itemId);

      // System.out.println("rs:" + rs.getRow());

      if (!rs.isBeforeFirst()) {
      	// System.out.println("return");
      	return "";
      }
      rs.first();

      // System.out.println("before fac");
      DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
      // System.out.println("fac");
      DocumentBuilder b = fac.newDocumentBuilder();
      org.w3c.dom.Document doc = b.newDocument();
      // System.out.println("doc");

      Element root = doc.createElement("Item");
      // System.out.println("root: " + doc.getElementById("Item"));
      root.setAttribute("ItemID", itemId);
      // System.out.println("root attr: " + root.getAttribute("ItemID"));
      doc.appendChild(root);
      // System.out.println("append: " + temp.hasAttributes());
      // System.out.println("root? " + doc.getDocumentElement());

      Element name = doc.createElement("Name");
      name.appendChild(doc.createTextNode(rs.getString("Name")));
      root.appendChild(name);

      Statement s_category = dbconn.createStatement();
      ResultSet rs_category = s_category.executeQuery("SELECT Category FROM Category WHERE ItemID = " + itemId);
      while (rs_category.next()) {
          Element category = doc.createElement("Category");
          category.appendChild(doc.createTextNode(rs_category.getString("Category")));
          root.appendChild(category);
      }

      Element currently = doc.createElement("Currently");
      currently.appendChild(doc.createTextNode("$" + rs.getString("Currently")));
      root.appendChild(currently);

      Element buy_price = doc.createElement("Buy_Price");
      if (rs.getDouble("Buy_Price") != 0) {
          buy_price.appendChild(doc.createTextNode("$" + rs.getString("Buy_Price")));
          root.appendChild(buy_price);
      }

      Element first_bid = doc.createElement("First_Bid");
      first_bid.appendChild(doc.createTextNode("$" + rs.getString("First_Bid")));
      root.appendChild(first_bid);

      Element number_of_bids = doc.createElement("Number_of_Bids");
      number_of_bids.appendChild(doc.createTextNode(rs.getString("Number_of_Bids")));
      root.appendChild(number_of_bids);

      // BIDS
      Element bids = doc.createElement("Bids");

      Statement s_bids = dbconn.createStatement();
      ResultSet rs_bids = s_bids.executeQuery("SELECT * FROM Bid WHERE ItemID = " + itemId);
      String user_id;
      while (rs_bids.next()) {
        Element bid = doc.createElement("Bid");

        // BIDDER
        user_id = rs_bids.getString("UserID");
        Statement s_bidder = dbconn.createStatement();
        ResultSet rs_bidder = s_bidder.executeQuery("SELECT * FROM Bidder WHERE UserID = '" + user_id + "'");
        rs_bidder.first();

        Element bidder = doc.createElement("Bidder");
        bidder.setAttribute("Rating", rs_bidder.getString("Rating"));
        bidder.setAttribute("UserID", rs_bidder.getString("UserID"));

        Element bidder_location = doc.createElement("Location");
        bidder_location.appendChild(doc.createTextNode(rs_bidder.getString("Location")));
        bidder.appendChild(bidder_location);

        Element bidder_country = doc.createElement("Country");
        bidder_country.appendChild(doc.createTextNode(rs_bidder.getString("Country")));
        bidder.appendChild(bidder_country);

        // BID
        bid.appendChild(bidder);

        Element bid_time = doc.createElement("Time");
        bid_time.appendChild(doc.createTextNode(sqlToXmlTime(rs_bids.getString("Time")))); //CHECK TIME?!?!
        bid.appendChild(bid_time);

        Element bid_amount = doc.createElement("Amount");
        bid_amount.appendChild(doc.createTextNode("$" + rs_bids.getString("Amount")));
        bid.appendChild(bid_amount);

        bids.appendChild(bid);
      }
      root.appendChild(bids);

      // LOCATION
      Element location = doc.createElement("Location");
      location.appendChild(doc.createTextNode(rs.getString("Location")));

      if (!(rs.getString("Latitude") == null || "".equals(rs.getString("Latitude"))) && !(rs.getString("Longitude") == null || "".equals(rs.getString("Longitude"))) && !rs.getString("Latitude").equalsIgnoreCase("0") && !rs.getString("Longitude").equalsIgnoreCase("0") ) {
          location.setAttribute("Latitude", rs.getString("Latitude"));
          location.setAttribute("Longitude", rs.getString("Longitude"));
      }
      root.appendChild(location);


      Element country = doc.createElement("Country");
      country.appendChild(doc.createTextNode(rs.getString("Country")));
      root.appendChild(country);

      Element started = doc.createElement("Started");
      started.appendChild(doc.createTextNode(sqlToXmlTime(rs.getString("Started"))));
      root.appendChild(started);

      Element ends = doc.createElement("Ends");
      ends.appendChild(doc.createTextNode(sqlToXmlTime(rs.getString("Ends"))));
      root.appendChild(ends);

      Element seller = doc.createElement("Seller");
      Statement s_seller = dbconn.createStatement();
      ResultSet rs_seller = s_seller.executeQuery("SELECT * FROM Seller WHERE UserID = '" + rs.getString("UserID") + "'");
      rs_seller.first();
      seller.setAttribute("Rating", rs_seller.getString("Rating"));
      seller.setAttribute("UserID", rs_seller.getString("UserID"));
      root.appendChild(seller);

      Element description = doc.createElement("Description");
      description.appendChild(doc.createTextNode(rs.getString("Description")));
      root.appendChild(description);

      TransformerFactory tfInstance = TransformerFactory.newInstance();
      Transformer transformerInstance = tfInstance.newTransformer();
      transformerInstance.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      StringWriter writer = new StringWriter();
      transformerInstance.transform(new DOMSource(doc), new StreamResult(writer));
      xmlstore = writer.getBuffer().toString();


      s.close();
    } catch (SQLException ex){
      // System.out.println(e);
      System.out.println("SQLException caught");
      System.out.println("---");
      while ( ex != null ){
          System.out.println("Message   : " + ex.getMessage());
          System.out.println("SQLState  : " + ex.getSQLState());
          System.out.println("ErrorCode : " + ex.getErrorCode());
          System.out.println("---");
          ex = ex.getNextException();
      }
    } catch (Exception e) {
        System.out.println(e);
    } finally {
        return xmlstore;
    }
	}

	public String echo(String message) {
		return message;
	}

}
