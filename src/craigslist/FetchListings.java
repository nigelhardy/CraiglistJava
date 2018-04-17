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
	public static int SIM_THRESH = 10;
	static ListingDB db;
	static Vector<Listing> listings = new Vector<Listing>();
	static Vector<SearchPage> search_pages = new Vector<SearchPage>();
	static Vector<String> listing_urls = new Vector<String>();
	
	static LevenshteinDistance lev_dist = null;
	
	static void build_urls()
	{
		
	}
	public static void init()
	{
		db = new ListingDB();
		lev_dist = new LevenshteinDistance(100);
	}
	public FetchListings()
	{
		
	}
	
	public static void get_listings(String region, String query, Integer page)
	{
		String url = "https://" + region + ".craigslist.org/search/cta?s=" + page.toString() + "&sort=date&query=" + query;
		System.out.println("Getting " + url);
		try {
			File search_page = new File("test_pages/" + region + page.toString());
			Document doc = null;
			if(search_page.exists())
			{
				doc = Jsoup.parse(search_page, "UTF-8");
			}
			else
			{
				doc = Jsoup.connect(url).get();
				downloadPageSearch(doc, url, region, page);
			}
			Elements results = doc.select("p.result-info");
			for (Element p_elem : results) 
	        {
				Elements res_links = p_elem.select("a.result-title");
				
				for(Element a_elem : res_links) 
				{
					String listing_url = a_elem.attr("href");
					listing_urls.add(listing_url);
				}				
	        }
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Exception when traversing url: " + url);
		}
	}
	public static void parse_listing(String listing_url) throws Exception
	{
		Document listing_doc = null;
		String[] url_parts = listing_url.split("/");
		File listing_page = new File("test_pages/" + url_parts[url_parts.length-1]);
		if(listing_page.exists())
		{
			listing_doc = Jsoup.parse(listing_page, "UTF-8");
		}
		else
		{
			listing_doc = Jsoup.connect(listing_url).get();
			downloadPage(listing_doc, listing_url);
		}
		String region = listing_url.split("//")[1].split("\\.")[0];
		Listing listing = new Listing(listing_doc, listing_url, region);
		save_listing(listing);
	}
	public static boolean save_listing(Listing new_listing)
	{
//		Iterator<Listing> iter = listings.iterator();
//		while(iter.hasNext())
//		{
//			Listing listing = iter.next();
//			int temp_ld = lev_dist.apply(new_listing.content, listing.content);
//			if(temp_ld < SIM_THRESH && temp_ld != -1)
//			{
//				return false;
//			}
//		}
		db.save_listing(new_listing);
		listings.add(new_listing);
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
			while(active_threads.size() < 4 && threads.size() > 0)
			{
				Thread temp_thread = threads.remove(0);
				active_threads.add(temp_thread);
				temp_thread.start();
			}
		}
		//f.send_best();
		//f.print_listings(100);	
	}
	public static void parse_listings()
	{
//		// create vector of threads to parse each search page
		Vector<Thread> threads = new Vector<Thread>();
		for(String _listing_url : listing_urls)
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
			while(active_threads.size() < 4 && threads.size() > 0)
			{
				Thread temp_thread = threads.remove(0);
				active_threads.add(temp_thread);
				temp_thread.start();
			}
		}
	}
	public static void main(String[] args) {
		String[] regions = {"monterey", "losangeles", "sfbay", "santabarbara", "orangecounty", "sacramento"};
		String[] queries = {"540i"};
		init();
		get_all_listings(regions, queries, 1);
		System.out.println("Size: " + listing_urls.size());
//		for (int i = 0; i < 20; i++) {
//			System.out.println(listing_urls.get(i));
//		}
		parse_listings();

		System.out.println("Size: " + listing_urls.size());
    }
}
