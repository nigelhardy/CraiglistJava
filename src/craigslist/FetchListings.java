package craigslist;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import craigslist.SearchPage.Category;

import java.util.*;

import org.apache.commons.io.FileUtils;

public class FetchListings {
	static int MAX_THREADS = 8;
	static int MAX_REQUESTS_PER_HOUR = 100;
	static boolean DEV_MODE = false;
	static ListingDB db;
	static Vector<Listing> listings = new Vector<Listing>();
	static Vector<SearchPage> search_pages = new Vector<SearchPage>();
	static Vector<String> listing_urls = new Vector<String>();
	static SendMail gmail = null;
	static int num_requests = 0;
	static int existing_requests = 0;
	static boolean OLD_REQ = false;
	
	public static void init()
	{
		db = new ListingDB();
		existing_requests = db.load_requests();
		System.out.println("Existing requests: " + Integer.toString(existing_requests));
		db.createSerialListingTable();
		gmail = new SendMail();
	}
	public FetchListings()
	{
		
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
		    Thread.sleep(250);
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
	public static Vector<SearchPage> generateSearchPages(String[] regions, String[] queries, int pages, Category category)
	{
		Vector<SearchPage> searchPages = new Vector<SearchPage>();
		// create vector of urls to search
		for(String region : regions)
		{
			for(String query : queries)
			{
				for(int i = 0; i < pages; i++)
				{
					searchPages.add(new SearchPage(region, query, i, category));
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
		Set<String> uniqueUrls = new HashSet<String> (allListingUrls);
		Vector<String> removeThese = new Vector<String>();
		if(uniqueUrls.size() > 0)
		{
			for(String uniqueUrl : uniqueUrls)
			{
				System.out.print("URL = " + uniqueUrl);
				if(db.all_urls.contains(uniqueUrl))
				{
					System.out.println(" DUPLICATE");
					// already in DB, ignore
					removeThese.add(uniqueUrl);
				}
				else
				{
					System.out.println();
				}
			}
		}
		for(String removeThis : removeThese)
		{
			uniqueUrls.remove(removeThis);
		}
		return new Vector<String>(uniqueUrls);

	}
	public static Vector<Listing> urlsToListings(Vector<String> listingUrls)
	{
		float totalListings = (float)listingUrls.size();
		float currentPercent = 0f;
		float percentMessage = .01f;
		float currentListing = 0f;
		Vector<Listing> listings = new Vector<Listing>();
		for(String listingUrl : listingUrls)
		{
			if(!DEV_MODE && existing_requests >= MAX_REQUESTS_PER_HOUR)
			{
				break;
			}
			else if(!DEV_MODE)
			{
				existing_requests++;
				try {
					listings.add(parseListing(listingUrl));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else
			{
				System.out.println(listingUrl);
				if(currentListing / totalListings > currentPercent)
				{
					System.out.println("Parsing " + Math.round(currentPercent*100) + "% done.");
					currentPercent += percentMessage;
				}
				try {
					listings.add(parseListing(listingUrl));
				} catch (Exception e) {
					e.printStackTrace();
				}		
			}
			currentListing++;
		}
		System.out.println("Parsing 100% done.");
		return listings;
	}
	public static void send_new_listings(Vector<Listing> goodListings)
	{
		if(goodListings.size() > 0)
		{
			String body = "New Sportwagen Listings:";
			for(Listing goodListing : goodListings)
			{
				body += "\n" + goodListing.title + ": " + goodListing.url;
			}
			System.out.println(body);
			//gmail.send_notification("New posts on CraigsList", body);
		}
	}
	public static void main(String[] args) {
		// Regions to search
		// ex: monterey, sfbay, losangeles, orangecounty
		// bakersfield, sacramento, slo, sandiego
//		String[] regions = {"sfbay"};
		String[] regions = {"losangeles", "orangecounty", 
				"monterey", "sfbay", "sacramento",
				"bakersfield", "slo", "sandiego"};

		// search query (words you type into the search bar)
//		String[] queries = {"sportwagen", "sportwagon", "jetta wagon", "wagon", "dodge magnum",
//				"srt8", "325it", "328it", "328i", "325i", "bmw 328", "bmw 325", "e46 wagon",
//				"328", "bmw touring", "bmw estate", "vw wagon", "volkswagen wagon", "vw jsw",
//				"jsw", "golf wagon", "audi avant", "audi wagon", "subaru legacy wagon", 
//				"legacy wagon", "legacy gt wagon", "mercedes wagon", "mercedes estate",
//				"subaru wagon", "impreza wagon", "wrx wagon"};
		String[] queries = {"sportwagen", "sportwagon", "jetta wagon", "vw wagon", "wolkwagen wagon",
				"wagon", "sportswagen", "sportswagon", "jsw"};
		// pages to get per query
		int pages = 1;
		int max_results = 50000;
		// initialize the database
		init();
		// pull new listings from craigslist? (false to use ones from DB)
		boolean getNewListings = true;
		if(getNewListings)
		{
			if(DEV_MODE)
				db.deleteOldListings();
			Vector<SearchPage> searchPages = generateSearchPages(regions, queries, pages, Category.CARS_AND_TRUCKS);
			System.out.println("Generated " + searchPages.size() + " search pages.");
			Vector<String> listingUrls = searchPageToListingUrl(searchPages);
			System.out.println("Generated " + listingUrls.size() + " listings.");
			Vector<Listing> listings = urlsToListings(listingUrls);
			System.out.println("Created " + listings.size() + " listings.");
			db.createSerialListingTable();
			db.saveSerialListings(listings);
		}

		Vector<Listing> listings = db.loadSerialListings();
		System.out.println("Loaded listings from DB");
		for(Listing listing : listings)
		{
			listing.set_sportwagen_value();
		}
		System.out.println("Done settings values.");
		// sort by value (highest first)
		listings.sort(new ValueSorter());
		Collections.reverse(listings);
		if(DEV_MODE)
		{
			String fileName = "craigslist-result.html";
		    String header = "<html><body><h1>Wagon Finder</h1>";
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
		}
		Vector<Listing> goodListings = new Vector<Listing>();
		Vector<String> goodListingUrls = new Vector<String>();
        Vector<String> allListingUrls = new Vector<String>();
		for(Listing listing : listings)
		{
        	if(!db.all_urls.contains(listing.url))
        	{
            	allListingUrls.add(listing.url);
    			if(listing.value > 1.5f)
    			{
    				goodListings.add(listing);
    				db.save_listing(listing);
    				goodListingUrls.add(listing.url);
    			}
        	}

		}
		db.save_listing_urls(allListingUrls);
		send_new_listings(goodListings);
		
		if(!DEV_MODE)
		{
			String fileName = "craigslist-result.html";
		    String header = "<html><body><h1>Wagon Finder</h1>";
		    String footer = "</body></html>";
		    BufferedWriter writer;
			try {
				writer = new BufferedWriter(new FileWriter(fileName));
				writer.append(header);
				for(int i = 0; i < Math.min(goodListings.size(), max_results); i++)
				{
					Listing listing = goodListings.get(i);
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
		}
		System.out.println("Done");
    }
}
