package craigslist;

import java.io.File;
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
		gmail = new SendMail();
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
	public static synchronized void add_listing(String listing_url)
	{
		if(!db.all_urls.contains(listing_url) && !listing_urls.contains(listing_url) && listing_urls.size() + get_requests() + existing_requests < MAX_REQUESTS_PER_HOUR)
		{
			listing_urls.add(listing_url);
		}
	}
	public static void get_listings(String region, String query, Integer page)
	{
		String url = "https://" + region + ".craigslist.org/search/cta?s=" + page.toString() + "&sort=date&query=" + query;
		System.out.println("Getting " + url);
		try {
			Document doc = get_doc(url);
			if(doc == null)
			{
				System.out.println("Too many recent requests.");
				return;
			}
			
			Elements results = doc.select("p.result-info");
			for (Element p_elem : results) 
	        {
				Elements res_links = p_elem.select("a.result-title");
				
				for(Element a_elem : res_links) 
				{
					String listing_url = a_elem.attr("href");
					add_listing(listing_url);
				}				
	        }
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Exception when traversing url: " + url);
		}
	}
	public static void parse_listing(String listing_url) throws Exception
	{
		String[] url_parts = listing_url.split("/");

		Document listing_doc = get_doc(listing_url);
		if(listing_doc == null)
		{
			System.out.println("Too many recent requests.");
			return;
		}
		
		Listing listing = new Listing(listing_doc);
		save_listing(listing);
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
	public static void get_all_listings(String[] regions, String[] queries, int pages)
	{		
		// create vector of urls to search
		for(String region : regions)
		{
			for(String query : queries)
			{
				for(int i = 0; i < pages; i++)
				{
					search_pages.add(new SearchPage(region, query, i));
				}
			}
		}
		// create vector of threads to parse each search page
		Vector<Thread> threads = new Vector<Thread>();
		for(SearchPage sp : search_pages)
		{
			Runnable obj = new Runnable()
			{
				public void run()
				{
					SearchPage sp = search_pages.remove(0);
					get_listings(sp.region, sp.query, sp.page);
				}
			};
			threads.addElement(new Thread(obj));
		}
		Vector<Thread> active_threads = new Vector<Thread>();
		while(threads.size() > 0 || active_threads.size() > 0)
		{
			// remove dead threads
			Iterator<Thread> thread_iter = active_threads.iterator();
			while(thread_iter.hasNext())
			{
				Thread temp_thread = thread_iter.next();
				if(!temp_thread.isAlive())
				{
					thread_iter.remove();
				}
			}
			// run at most four threads at a time (not including main thread)
			while(active_threads.size() < MAX_THREADS && threads.size() > 0)
			{
				Thread temp_thread = threads.remove(0);
				active_threads.add(temp_thread);
				temp_thread.start();
			}
		}
	}
	public static void parse_listings()
	{
//		// create vector of threads to parse each search page
		Vector<Thread> threads = new Vector<Thread>();
		for(int i = 0; i < listing_urls.size(); i++)
		{
			Runnable obj = new Runnable()
			{
				public void run()
				{
					if(listing_urls.size() > 0)
					{
						String listing_url = listing_urls.remove(0);
						try {
							parse_listing(listing_url);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
				}
			};
			threads.addElement(new Thread(obj));
		}
		Vector<Thread> active_threads = new Vector<Thread>();
		while(threads.size() > 0 || active_threads.size() > 0)
		{
			// remove dead threads
			Iterator<Thread> thread_iter = active_threads.iterator();
			while(thread_iter.hasNext())
			{
				Thread temp_thread = thread_iter.next();
				if(!temp_thread.isAlive())
				{
					thread_iter.remove();
				}
			}
			// run at most four threads at a time (not including main thread)
			while(active_threads.size() < MAX_THREADS && threads.size() > 0)
			{
				Thread temp_thread = threads.remove(0);
				active_threads.add(temp_thread);
				temp_thread.start();
			}
		}
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
		long start = System.nanoTime();
		// Regions to search
		String[] regions = {"monterey", "losangeles", "sfbay", "santabarbara", "orangecounty", "sacramento"};
		String[] queries = {"540i"};
		init();
		get_all_listings(regions, queries, 1);
		db.save_listing_urls(new Vector<String>(listing_urls));
		System.out.println("done");
		parse_listings();
		db.log_num_requests(num_requests);
		long end = System.nanoTime();
		System.out.println("Took " + (end-start)/1000000000f + " sec to get and parse listings.");
		start = System.nanoTime();
		send_new_listings();
		end = System.nanoTime();
		System.out.println("Took " + (end-start)/1000000000f + " sec to send email.");
    }
}
