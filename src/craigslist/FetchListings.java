package craigslist;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;

import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.text.similarity.*;

public class FetchListings {
	static int MAX_THREADS = 8;
	static int MAX_REQUESTS_PER_HOUR = 250;
	static ListingDB db;
	static Vector<Listing> listings = new Vector<Listing>();
	static Vector<SearchPage> search_pages = new Vector<SearchPage>();
	static Vector<String> listing_urls = new Vector<String>();
	static SendMail gmail = null;
	static LevenshteinDistance lev_dist = null;
	static int num_requests = 0;
	static int existing_requests = 0;
	static boolean OLD_REQ = false;
	
	public static void init()
	{
		db = new ListingDB();
		existing_requests = db.load_requests();
		lev_dist = new LevenshteinDistance(100);
		//gmail = new SendMail();
	}
	public FetchListings()
	{
		
	}
	public static synchronized void inc_requests()
	{
		num_requests++;
	}
	public static synchronized int get_requests()
	{
		return num_requests;
	}
	public static Document get_doc(String url)
	{
		if(get_requests() + existing_requests < MAX_REQUESTS_PER_HOUR)
		{
			try {
				inc_requests();
				return Jsoup.connect(url).get();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
		}
		else
		{
			return null;
		}
	}
	public static Document getDocument(String url)
	{
		try {
			return Jsoup.connect(url).get();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	public static synchronized void add_listing(String listing_url)
	{
//		if(!db.all_urls.contains(listing_url) && !listing_urls.contains(listing_url) && listing_urls.size() + get_requests() + existing_requests < MAX_REQUESTS_PER_HOUR)
//		{
//			listing_urls.add(listing_url);
//		}
		listing_urls.add(listing_url);

	}
	public static void getListings(SearchPage searchPage, Vector<String> listingUrls)
	{
		System.out.println("Getting " + searchPage.get_url());
		try {
			Document doc = getDocument(searchPage.get_url());
			
			Elements results = doc.select("p.result-info");
			for (Element p_elem : results) 
	        {
				Elements res_links = p_elem.select("a.result-title");
				
				for(Element a_elem : res_links) 
				{
					String listing_url = a_elem.attr("href");
					listingUrls.add(listing_url);
				}		
	        }
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Exception when traversing url: " + searchPage.get_url());
		}
	}
	public static Listing parseListing(String listingUrl) throws Exception
	{
		Document listingDoc = getDocument(listingUrl);
		try
		{
		    Thread.sleep(100);
		}
		catch(InterruptedException ex)
		{
		    Thread.currentThread().interrupt();
		}
		return new Listing(listingDoc);
	}
	public static boolean save_listing(Listing new_listing)
	{
		if(new_listing.determine_value() >= 0)
		{
			if(db.save_listing(new_listing))
			{
				listings.add(new_listing);
			}
			
		}
		
		return true;
	}
	public void print_listings(int max)
	{
		Integer num_listings_print = max;
		Integer counter = 0;
		for(Listing listing : listings)
		{
			counter += 1;
			if(counter > num_listings_print)
			{
				break;
			}
			System.out.println(listing.value + ": " + listing.title + " - " + listing.region);
		}
	}
	public static void downloadPage(Document doc, String url) throws Exception {
		String[] url_parts = url.split("/");
        final File f = new File("test_pages/" + url_parts[url_parts.length-1]);
        FileUtils.writeStringToFile(f, doc.outerHtml(), "UTF-8");
    }
	public static void downloadPageSearch(Document doc, String url, String region, Integer page) throws Exception {
        final File f = new File("test_pages/" + region + page.toString());
        FileUtils.writeStringToFile(f, doc.outerHtml(), "UTF-8");
    }
	public static Vector<SearchPage> generateSearchPages(String[] regions, String[] queries, int pages)
	{
		Vector<SearchPage> searchPages = new Vector<SearchPage>();
		// create vector of urls to search
		for(String region : regions)
		{
			for(String query : queries)
			{
				for(int i = 0; i < pages; i++)
				{
					searchPages.add(new SearchPage(region, query, i));
				}
			}
		}
		return searchPages;
	}
	public static Vector<String> searchPageToListingUrl(Vector<SearchPage> searchPages)
	{
		Vector<String> allListingUrls = new Vector<String>();
		for(SearchPage sp : searchPages)
		{
			getListings(sp, allListingUrls);
		}
		return allListingUrls;
	}
	public static Vector<Listing> urlsToListings(Vector<String> listingUrls)
	{
		Vector<Listing> listings = new Vector<Listing>();
		for(int i = 0; i < listingUrls.size(); i++)
		{
			try {
				listings.add(parseListing(listingUrls.get(i)));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return listings;
	}
	public static void send_new_listings()
	{
		if(listings.size() > 0)
		{
			String body = "New Posts:";
			for(Listing listing : listings)
			{
				body += "\n" + listing.title + ": " + listing.url;
			}
			gmail.send_notification("New 540i posts on CraigsList", body);
		}
	}
	public static void main(String[] args) {
		// Regions to search
		// ex: monterey, sfbay, losangeles, orangecounty
		// bakersfield, sacramento, slo, sandiego
		String[] regions = {"monterey", "sfbay", "losangeles"};
		// search query (words you type into the search bar)
		String[] queries = {"wagon"};
		// pages to get per query
		int pages = 3;
		int max_results = 50000;
		// initialize the database
		init();
		// pull new listings from craigslist? (false to use ones from DB)
		boolean getNewListings = false;
		if(getNewListings)
		{
			db.deleteOldListings();
			Vector<SearchPage> searchPages = generateSearchPages(regions, queries, pages);
			System.out.println(searchPages.size());
			Vector<String> listingUrls = searchPageToListingUrl(searchPages);
			System.out.println(listingUrls.size());
			Vector<Listing> listings = urlsToListings(listingUrls);
			System.out.println(listings.size());
			db.createSerialListingTable();
			for(int i = 0; i < listings.size(); i++)
			{
				db.saveSerialListing(listings.get(i));
			}
		}

		Vector<Listing> listings = db.loadSerialListings();
		System.out.println(listings.size());
		for(int i = 0; i < listings.size(); i++)
		{
			listings.get(i).set_wagon_value();
		}
		// sort by value (highest first)
		listings.sort(new ValueSorter());
		Collections.reverse(listings);
		String fileName = "craigslist-result.html";
	    String str = "World";
	    String header = "<html><body><h1>Emmo Tries Computers</h1>";
	    String footer = "</body></html>";
	    BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(fileName));
			writer.append(header);
			for(int i = 0; i < Math.min(listings.size(), max_results); i++)
			{
				Listing listing = listings.get(i);
				writer.append("<br><a target=\"_blank\" href=\"");
				writer.append(listing.url);
				writer.append("\">" + listing.getValue() + " - " + listing.title + "</a>");
			}
		    writer.append(footer);
		     
		    writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		System.out.println("Done");
    }
}
