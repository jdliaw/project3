package edu.ucla.cs.cs144;

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
	
	public SearchResult[] basicSearch(String query, int numResultsToSkip, 
			int numResultsToReturn) {
		
			// ArrayList<SearchResult> result = new ArrayList<SearchResult>();
			// instantiate the search engine
			SearchEngine se = null;
			TopDocs topDocs = null;
			int numTotalQueries = numResultsToSkip + numResultsToReturn;
			try {
				se = new SearchEngine();
				topDocs = se.performSearch(query, numTotalQueries); 
			} catch (Exception e) {
				;
			}

			// obtain the ScoreDoc (= documentID, relevanceScore) array from topDocs
			ScoreDoc[] hits = topDocs.scoreDocs;
			int resultSize;
			SearchResult[] result = new SearchResult[hits.length];
			
			// retrieve each matching document from the ScoreDoc arry
			//i = numresults to skip..?
			for (int i = numResultsToSkip; i < hits.length; i++) {
					Document doc =  null;		
					try {
						doc = se.getDocument(hits[i].doc);
					}catch(IOException e) {
						System.err.println("IOException");
					}
					String description = doc.get("description");
					String name = doc.get("name");
					String id = doc.get("id");
					// if(description == null || name == null || id == null) {
					// 	continue;
					// }
					result[i] = new SearchResult(id, name);
					// result.add(new SearchResult(id, name));
					// System.out.println("Id, name: " + id + ", " + name + "\nDescription: " + description);
					
			}
			System.out.println("Hits.length: " + hits.length);

			
			return result;
	}
	/*
"superman": 68 matches
"kitchenware": 1462
"star trek": 770
	*/

	public SearchResult[] spatialSearch(String query, SearchRegion region,
			int numResultsToSkip, int numResultsToReturn) {
		// TODO: Your code here!
		return new SearchResult[0];
	}

	public String getXMLDataForItemId(String itemId) {
		// TODO: Your code here!
		return "";
	}
	
	public String echo(String message) {
		return message;
	}

}
