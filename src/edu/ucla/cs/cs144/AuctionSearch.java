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
            topDocs = se.performSearch(query, numTotalQueries); 										//search
        } catch (Exception e) {
            System.err.println("error");
        }

        // obtain the ScoreDoc (= documentID, relevanceScore) array from topDocs
        ScoreDoc[] hits = topDocs.scoreDocs;

        // At this point, we have performed our search and gotten the top docs that we want. This
        // is scored and then stored in a variable called hits.

        // Now, we need to determine the size of our result size. This can be done with a little bit of math due to
        // hits.length, numResultsToSkip, and numResultsToReturn.
        int maxResultSize;

        //if we're skipping more than the docs we have, we skip them all, so 0
        if (hits.length - numResultsToSkip < 0) {
            maxResultSize = 0;
        }
        //return a max of how many hits we got minus the number we skip
        else if (hits.length - numResultsToSkip < numResultsToReturn) {
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
            Document doc = null;
            try {
                doc = se.getDocument(hits[i].doc);
            } catch (IOException e) {
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
                String geom = "GeomFromText('Polygon((" + region.getLx() + " " + region.getLy() + ", " + region.getLx() + " " + region.getRy() + ", " + region.getRx() + " " + region.getRy() + ", " + region.getRx() + " " + region.getLy() + ", " + region.getLx() + " " + region.getLy() + "))')";

                //See the above comment...
                Statement s = conn.createStatement();
                ResultSet rs = s.executeQuery("SELECT ItemID FROM Location WHERE MBRContains("
                    + geom + ",Position);");

                //loop through results and add our item ids
                while(rs.next()) {
                    spatial_rs.add(rs.getInt("ItemID"));
                }

                // close connections
                s.close();
                rs.close();
                conn.close();

            }
            catch (SQLException e) {
                System.err.println("SQLException");
            }

            SearchResult[] basic_search = basicSearch(query, 0, spatial_rs.size());
            ArrayList<SearchResult> results = new ArrayList<SearchResult>();

            int skippedResults = 0;
            for(SearchResult r: basic_search) {
                if (numResultsToReturn < 0) {
                    break;
                }
                if (spatial_rs.contains(Integer.parseInt(r.getItemId()))) {
                    if (skippedResults < numResultsToSkip) {
                        skippedResults++;
                        continue;
                    }
                    results.add(r);
                }
            }
            int size = results.size();
            return results.toArray(new SearchResult[size]);
        }

    public String sqlToXmlTime(String sqlTime) {
        //Set date formats so we can convert b/n sql and our new xml.
        SimpleDateFormat sqlDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat xmlDateFormat = new SimpleDateFormat("MMM-dd-yy HH:mm:ss");
        Date date = null;
        try {
            date = sqlDateFormat.parse(sqlTime);
        }
        catch (Exception e) {
            System.err.println("Exception with dates");
        }
        return xmlDateFormat.format(date);
    }

    public String getXMLDataForItemId(String itemId) {
        String xmldata = "";
        Connection conn = null;
        // Create a connection to the database
        try {
            // Connection to db manager
            conn = DbManager.getConnection(true);
            Statement s = conn.createStatement();
            ResultSet rs = s.executeQuery("SELECT * FROM Item " + "WHERE ItemID = " + itemId);

            if (!rs.isBeforeFirst()) {
                return "";
            }
            //set cursor at first result
            rs.first();

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            org.w3c.dom.Document doc = factory.newDocumentBuilder().newDocument();

/*
<Item ItemID="1043402767">
        <Name>Precious Moments Girl Stove Mini Tea Set</Name>
        <Category>Collectibles</Category>
        <Category>Decorative &amp; Holiday</Category>
        <Category>Decorative by Brand</Category>
        <Category>Enesco</Category>
        <Category>Precious Moments</Category>
        <Currently>$4.00</Currently>
        <First_Bid>$4.00</First_Bid>
        <Number_of_Bids>1</Number_of_Bids>
        <Bids>
          <Bid>
            <Bidder Rating="2919" UserID="goldcoastvideo">
              <Location>Los Angeles,CA</Location>
              <Country>USA</Country>
            </Bidder>
            <Time>Dec-06-01 06:44:54</Time>
            <Amount>$4.00</Amount>
          </Bid>
        </Bids>
        <Location Latitude="43.017412" Longitude="-87.569664">MIlwaukee Wi</Location>
        <Country>USA</Country>
        <Started>Dec-03-01 18:44:54</Started>
        <Ends>Dec-13-01 18:44:54</Ends>
        <Seller Rating="952" UserID="fallsantiques" />
        <Description>PRECIOUS MOMENTS GIRL/STOVE MINI TEA SET: This really cute little mini decorative teaset is mint in box. It was only removed for photo. It is about 6 inches across. Original price was $20.00. Buyer to pay postage and handling of $4.50 for delivery in USA.</Description>
    </Item>
*/


            //create element name Item, NOT Items
            //<Item ItemID="1043402767">
            Element root = doc.createElement("Item");
            root.setAttribute("ItemID", itemId);
            doc.appendChild(root);

            //<Name>Precious Moments Girl Stove Mini Tea Set</Name>
            Element name = doc.createElement("Name");
            name.appendChild(doc.createTextNode(rs.getString("Name")));
            root.appendChild(name);



/*
        Category>Collectibles</Category>
        <Category>Decorative &amp; Holiday</Category>
        <Category>Decorative by Brand</Category>
        <Category>Enesco</Category>
        <Category>Precious Moments</Category>
*/
            Statement st = conn.createStatement();
            //st cuz s is already defined, cat_rs for category
            ResultSet cat_rs = st.executeQuery("SELECT Category FROM Category WHERE ItemID = " + itemId);
            //loop through and append categories
            while (cat_rs.next()) {
                Element cat = doc.createElement("Category");
                cat.appendChild(doc.createTextNode(cat_rs.getString("Category")));
                root.appendChild(cat);
            }
            

            //<Currently>$4.00</Currently>
            Element currently = doc.createElement("Currently");
            String curr = "$" + rs.getString("Currently");
            currently.appendChild(doc.createTextNode(curr));
            root.appendChild(currently);

            Element buy_price = doc.createElement("Buy_Price");
            //check if null or 0
            if (rs.getDouble("Buy_Price") != 0) {
                String bp = "$" + rs.getString("Buy_Price");
                buy_price.appendChild(doc.createTextNode(bp));
                root.appendChild(buy_price);
            }

            //first bid
            Element first_bid = doc.createElement("First_Bid");
            String fb = "$" + rs.getString("First_Bid");
            first_bid.appendChild(doc.createTextNode(fb));
            root.appendChild(first_bid);
        
            //number of bids
            Element number_of_bids = doc.createElement("Number_of_Bids");
            number_of_bids.appendChild(doc.createTextNode(rs.getString("Number_of_Bids")));
            root.appendChild(number_of_bids);

            // BIDS
            Element bids = doc.createElement("Bids");
            Statement sb = conn.createStatement();
            ResultSet bids_rs = sb.executeQuery("SELECT * FROM Bid WHERE ItemID = " + itemId);
            String user_id;
            while (bids_rs.next()) {
                Element bid = doc.createElement("Bid");

                // BIDDER
                user_id = bids_rs.getString("UserID");
                Statement sbr = conn.createStatement();
                ResultSet bidder_rs = sbr.executeQuery("SELECT * FROM Bidder WHERE UserID = '" + user_id + "'");
                bidder_rs.first();

                Element bidder = doc.createElement("Bidder");
                bidder.setAttribute("Rating", bidder_rs.getString("Rating"));
                bidder.setAttribute("UserID", bidder_rs.getString("UserID"));

                //location then country
                Element loc = doc.createElement("Location");
                loc.appendChild(doc.createTextNode(bidder_rs.getString("Location")));
                bidder.appendChild(loc);

                Element country = doc.createElement("Country");
                country.appendChild(doc.createTextNode(bidder_rs.getString("Country")));
                bidder.appendChild(country);

                // BID
                bid.appendChild(bidder);

                Element time = doc.createElement("Time");
                time.appendChild(doc.createTextNode(sqlToXmlTime(bids_rs.getString("Time")))); //CHECK TIME?!?!
                bid.appendChild(time);

                Element amount = doc.createElement("Amount");
                amount.appendChild(doc.createTextNode("$" + bids_rs.getString("Amount")));
                bid.appendChild(amount);

                bids.appendChild(bid);
                sbr.close();
            }
            root.appendChild(bids);

            // LOCATION
            Element loc = doc.createElement("Location");
            loc.appendChild(doc.createTextNode(rs.getString("Location")));
            boolean latitude_and_longitude_isValid = !(rs.getString("Latitude") == null || "".equals(rs.getString("Latitude"))) && !(rs.getString("Longitude") == null || "".equals(rs.getString("Longitude"))) && !rs.getString("Latitude").equalsIgnoreCase("0") && !rs.getString("Longitude").equalsIgnoreCase("0");

            if (latitude_and_longitude_isValid) {
                loc.setAttribute("Latitude", rs.getString("Latitude"));
                loc.setAttribute("Longitude", rs.getString("Longitude"));
            }
            root.appendChild(loc);


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

            Statement ss = conn.createStatement();
            ResultSet seller_rs = ss.executeQuery("SELECT * FROM Seller WHERE UserID = '" + rs.getString("UserID") + "'");
            seller_rs.first();
            seller.setAttribute("Rating", seller_rs.getString("Rating"));
            seller.setAttribute("UserID", seller_rs.getString("UserID"));
            root.appendChild(seller);

            Element description = doc.createElement("Description");
            description.appendChild(doc.createTextNode(rs.getString("Description")));
            root.appendChild(description);

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            xmldata = writer.getBuffer().toString();

            //close statements and stuff
            s.close();
            st.close();
            sb.close();
            ss.close();
            rs.close();
            cat_rs.close();
            bids_rs.close();
            seller_rs.close();  
            conn.close();

        } catch (SQLException e) {
            System.err.println("SQLException");
        } 
        finally {
            return xmldata;
        }
    }

	public String echo(String message) {
        return message;
    }

}
