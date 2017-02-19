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

		HashSet<Integer> spatialResultSet = new HashSet<Integer>();

    try {
      Connection conn = DbManager.getConnection(true);

      int item_id;

      String polygon = "'POLYGON((" + region.getRx() + " " + region.getRy() + ", "
          + region.getRx() + " " + region.getLy() + ", "
          + region.getLx() + " " + region.getLy() + ", "
          + region.getLx() + " " + region.getRy() + ", "
          + region.getRx() + " " + region.getRy() + "))'";

      Statement s = conn.createStatement();
      ResultSet rs = s.executeQuery("SELECT ItemID FROM Location WHERE MBRContains(GeomFromText("
          + polygon + "),Position);");

      while (rs.next()) {
          item_id = rs.getInt("ItemID");
          spatialResultSet.add(item_id);
      }

      // close connections
      s.close();
      rs.close();
      conn.close();

		} catch (SQLException ex){
      System.out.println("SQLException caught");
      System.out.println("---");
      while ( ex != null ){
          System.out.println("Message   : " + ex.getMessage());
          System.out.println("SQLState  : " + ex.getSQLState());
          System.out.println("ErrorCode : " + ex.getErrorCode());
          System.out.println("---");
          ex = ex.getNextException();
      }
    }

    // System.out.println("spatial size: " + spatialResultSet.size());
    SearchResult[] basicSearchResults = basicSearch(query, 0, spatialResultSet.size());
    ArrayList<SearchResult> results = new ArrayList<SearchResult>();
    int i = 0;

    for (SearchResult r: basicSearchResults) {
        if (results.size() >= numResultsToReturn)
            break;

        if(spatialResultSet.contains(Integer.parseInt(r.getItemId()))) {
            if (i < numResultsToSkip) {
                i++;
                continue;
            }
            results.add(r);
        }
    }
		return results.toArray(new SearchResult[results.size()]);


		// TODO: Your code here!
		// if(numResultsToReturn <= 0 || numResultsToSkip < 0) {
		// 	System.err.println("Invalid input!");
		// 	return new SearchResult[0];
		// }

		// int firstResult = 0;
		// int skippedResults = 0;
		// int addedResults = 0;

		// ArrayList<SearchResult> results = new ArrayList<SearchResult>();
		// SearchResult[] basic_search = basicSearch(query, 0, numResultsToReturn);

		// Connection conn = null;
		// try {
		// 	conn = DbManager.getConnection(true);
		// 	Statement s = conn.createStatement();

		// 	String polygon = "GeomFromText('Polygon((" +
		// 	region.getLx() + " " + region.getLy() + ", " +
		// 	region.getLx() + " " + region.getRy() + ", " +
		// 	region.getRx() + " " + region.getRy() + ", " +
		// 	region.getRx() + " " + region.getLy() + ", " +
		// 	region.getLx() + " " + region.getLy() +  "))')";

		// 	PreparedStatement checkContains = conn.prepareStatement("SELECT MBRContains(" + polygon +
		// 	 ",Position) AS isContained FROM Location WHERE ItemID = ?");

		// 	while(addedResults < numResultsToReturn && basic_search.length > 0) {
		// 		for(int i = 0; i < basic_search.length; i++) {
		// 			String itemId = basic_search[i].getItemId();
		// 			checkContains.setString(1, itemId);
		// 			ResultSet contains_rs = checkContains.executeQuery();

		// 			if(contains_rs.next() && contains_rs.getBoolean("isContained")) {
		// 				if(addedResults >= numResultsToReturn) {
		// 					break;
		// 				}
		// 				else if (skippedResults >= numResultsToSkip) {
		// 					results.add(basic_search[i]);
		// 					addedResults++;
		// 				}
		// 				else {
		// 					skippedResults++;
		// 				}
		// 			}
		// 			contains_rs.close();
		// 		}
		// 			firstResult += numResultsToReturn;
		// 			basic_search = basicSearch(query, firstResult, numResultsToReturn);
		// 	}
		// 	checkContains.close();
		// 	conn.close();


		// }
		// catch (SQLException e) {
		// 	System.out.println(e);
		// }

		// SearchResult[] searchResults = new SearchResult[addedResults];
		// for(int i = 0; i < addedResults; i++) {
		// 	searchResults[i] = results.get(i);
		// }

		// return searchResults;
	}

	public String getXMLDataForItemId(String itemId) {
		// TODO: Your code here!
		return "";
	}

	public String echo(String message) {
		return message;
	}



}
