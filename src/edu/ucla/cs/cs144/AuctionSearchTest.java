package edu.ucla.cs.cs144;

import java.util.Calendar;
import java.util.Date;

import edu.ucla.cs.cs144.AuctionSearch;
import edu.ucla.cs.cs144.SearchRegion;
import edu.ucla.cs.cs144.SearchResult;
import java.io.IOException;
import org.apache.lucene.queryparser.classic.ParseException;

public class AuctionSearchTest {
	public static void main(String[] args1) throws IOException, ParseException
	{
		AuctionSearch as = new AuctionSearch();

		String message = "Test message";
		String reply = as.echo(message);
		System.out.println("Reply: " + reply);

		String query = "superman";
		SearchResult[] basicResults = as.basicSearch(query, 0, 4000);
		System.out.println("Basic Seacrh Query: " + query);
		System.out.println("Received " + basicResults.length + " results");//\nShould be 68 matches\n");

		query = "kitchenware";
		basicResults = null;
		basicResults = as.basicSearch(query, 0, 1500);
		System.out.println("Basic Seacrh Query: " + query);
		System.out.println("Received " + basicResults.length + " results");//\nShould be 1462 matches\n");



		query = "star trek";
		basicResults = null;
		basicResults = as.basicSearch(query, 0, 3000);
		System.out.println("Basic Seacrh Query: " + query);
		System.out.println("Received " + basicResults.length + " results\n");//\nShould be 770 matches\n");


		// "superman": 68 matches
// "kitchenware": 1462
// "star trek": 770

		// for(SearchResult result : basicResults) {
		// 	System.out.println(result.getItemId() + ": " + result.getName());
		// }

		SearchRegion region =
		    new SearchRegion(33.774, -118.63, 34.201, -117.38);
		SearchResult[] spatialResults = as.spatialSearch("camera", region, 0, 20);
		System.out.println("Spatial Seacrh");
		System.out.println("Received " + spatialResults.length + " results");
		for(SearchResult result : spatialResults) {
			System.out.println(result.getItemId() + ": " + result.getName());
		}

		String itemId = "1497595357";
		String item = as.getXMLDataForItemId(itemId);
		System.out.println("XML data for ItemId: " + itemId);
		System.out.println(item);

		// Add your own test here
	}
}
