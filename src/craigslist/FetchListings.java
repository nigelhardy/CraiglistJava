package craigslist;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
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
	static int MAX_REQUESTS_PER_RUN = 1000;
	static boolean DEV_MODE = true;
	static boolean GEN_HTML = true;
	static boolean GET_NEW_LISTINGS = false;

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
		//existing_requests = db.load_requests();
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
				if(db.all_urls.contains(uniqueUrl))
				{
					// already in DB, ignore
					removeThese.add(uniqueUrl);
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
			if(db.all_urls.contains(listingUrl))
			{
				continue;
			}
			if(!DEV_MODE && existing_requests >= MAX_REQUESTS_PER_RUN)
			{
				System.out.println("Hit max requests allowed per run");
				break;
			}
			else
			{
				if(currentListing / totalListings > currentPercent)
				{
					System.out.println("Parsing " + Math.round(currentPercent*100) + "% done.");
					currentPercent += percentMessage;
				}
				try {
					Listing l = parseListing(listingUrl);
					existing_requests++;
					listings.add(l);		
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
			Date now = new Date();
			SimpleDateFormat dateFormat = new SimpleDateFormat("E");
			dateFormat.setTimeZone(TimeZone.getDefault());
			String body = "New Sportwagen Listings: \n";
			for(Listing goodListing : goodListings)
			{
				body += goodListing.region + "\n" + goodListing.title + ": " + goodListing.url + "\n";
			}
			System.out.println(body);
			gmail.send_notification("New posts on CraigsList " + dateFormat.format(now), body);
		}
	}
	public static Vector<Listing> makeUnique(Vector<Listing> listings)
	{
		float totalListings = (float)listings.size();
		float currentPercent = 0f;
		float percentMessage = .01f;
		float currentListing = 0f;
		Vector<Listing> saveListings = new Vector<Listing>();
		for(Listing l : listings)
		{
			if(currentListing / totalListings > currentPercent)
			{
				System.out.println("Parsing " + Math.round(currentPercent*100) + "% done.");
				currentPercent += percentMessage;
			}
			try {
				
				if(db.is_unique(l, saveListings, true))
				{
					saveListings.add(l);
				}
				else
				{
					System.out.println("Duplicate!");
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		db.deleteOldListings();
		db.saveSerialListings(saveListings);
		return saveListings;

	}
	public static void genHtml(Vector<Listing> listings, int max_results)
	{
		String fileName = "craigslist-result.html";
	    String header = "<html><body><h1>Miata Finder</h1>";
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
				writer.append("\">" + listing.region + " " + listing.getValue() + " - " + listing.title + "</a>");
			}
		    writer.append(footer);
		     
		    writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static void main(String[] args) {
		// Regions to search
		// ex: monterey, sfbay, losangeles, orangecounty
		// bakersfield, sacramento, slo, sandiego
		String[] regions = {"sfbay", "monterey", "losangeles", "orangecounty",
				"bakersfield", "sacremento", "slo", "sandiego"};
		// search query (words you type into the search bar)
		String[] queries = {"miata", "mx5", "mx-5"};
		// pages to get per query
		if(args.length == 1)
		{
			try {
				MAX_REQUESTS_PER_RUN = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				System.out.println("Invalid argument for MAX_REQUESTS_PER_RUN");
				e.printStackTrace();
			}
		}
		int pages = 1;
		// initialize the database
		init();
		Vector<Listing> listings;
		// pull new listings from craigslist? (false to use ones from DB)
		if(GET_NEW_LISTINGS)
		{
			if(DEV_MODE)
				db.deleteOldListings();
			Vector<SearchPage> searchPages = generateSearchPages(regions, queries, pages, Category.CARS_AND_TRUCKS);
			System.out.println("Generated " + searchPages.size() + " search pages.");
			Vector<String> listingUrls = searchPageToListingUrl(searchPages);
			System.out.println("Generated " + listingUrls.size() + " listings.");
			listings = urlsToListings(listingUrls);
			System.out.println("Created " + listings.size() + " listings.");
			db.createSerialListingTable();
			if(DEV_MODE)
				// saves all listings for dev
				db.saveSerialListings(listings);
		}
		else
		{
			listings = db.loadSerialListings();

		}
		//listings = makeUnique(listings);
		System.out.println("Loaded " + Integer.toString(listings.size()) + " listings from DB");
		for(Listing listing : listings)
		{
			listing.set_miata_value();
		}
		System.out.println("Done setting values.");
		// sort by value (highest first)
		listings.sort(new ValueSorter());
		Collections.reverse(listings);
		

		Vector<Listing> dbListings = null;
		if(GET_NEW_LISTINGS)
		{
			dbListings = db.loadSerialListings();
		}
		Vector<Listing> goodListings = new Vector<Listing>();
        Vector<String> allListingUrls = new Vector<String>();
		for(Listing listing : listings)
		{
        	allListingUrls.add(listing.url);
			if(listing.value >= 0.f)
			{
				try {
					if(dbListings == null || db.is_unique(listing, dbListings, true))
					{
						System.out.println(listing);
						goodListings.add(listing);
						if(dbListings != null)
							dbListings.add(listing);
						db.save_listing(listing);
					}
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		if(GET_NEW_LISTINGS)
		{
			// if we found a duplicate with higher score we replace
			// it in dbListings, so instead of deleting one
			// and replacing it, we just delete all
			// add the new ones to a vector
			// and save all, it should never be TOO many
			// because we only save good ones
			db.deleteOldListings();
			db.saveSerialListings(dbListings);
			db.save_listing_urls(allListingUrls);
			db.log_num_requests(existing_requests);
		}

		if(!DEV_MODE)
		{
			send_new_listings(goodListings);
		}
		if(GEN_HTML)
			genHtml(goodListings, 1000);
		System.out.println("Done");
    }
}
