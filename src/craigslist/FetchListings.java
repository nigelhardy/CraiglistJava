package craigslist;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Vector;

import java.io.IOException;
import java.io.Reader;
import java.util.Properties;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;

public class FetchListings {
	Vector<Listing> listings = new Vector<Listing>();
	Vector<Listing> alert_listings = new Vector<Listing>();
	Map<String, String> config;
	public FetchListings()
	{
		Scanner in;
		try {
			in = new Scanner(new FileReader("config/gmail-config.txt"));
			while(in.hasNext())
			{
				String[] config_value = in.next().split("=");
				
				if(config_value.length == 2)
				{
					config.put(config_value[0], config_value[1]);
				}
			}
			in.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Could not read config file.");
		}
		
	}
	
	public void get_listings(String url)
	{
		try {
			Document doc = Jsoup.connect(url).get();
			Elements results = doc.select("p.result-info");
			for (Element p_elem : results) 
	        {
				Elements res_links = p_elem.select("a.result-title");
				
				for(Element a_elem : res_links) 
				{
					String listing_url = a_elem.attr("href");
					Document listing_doc = Jsoup.connect(listing_url).get();
					create_listing(listing_doc, listing_url);
					
				}
	        }
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Problem url: " + url);
		}
	}
	public void create_listing(Document listing_doc, String listing_url)
	{
		Elements title = listing_doc.select("#titletextonly");
		Elements content = listing_doc.select("#postingbody");
		Listing listing = new Listing();
		
		if(listing_url.contains("/cto/"))
		{
			// cto is craiglist abrev for cars/trucks by owner
			// ctd is dealership
			// cta in search is all
			listing.by_owner = true;
		}
		else
		{
			listing.by_owner = false;
		}
		
		if(title.size() > 0)
		{
			try 
			{
				listing.title = title.text().toLowerCase();
				listing.content = content.text().replace(content.select(".print-qrcode-label").text(), "").trim().toLowerCase();
				Elements attributes = listing_doc.select("p.attrgroup:gt(0)").select("span");
				listing.attr_make_model = listing_doc.select("p.attrgroup:eq(0) span:eq(0)").text();
								
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
				LocalDateTime formatDateTime = LocalDateTime.parse(listing_doc.select(".date[datetime]").attr("datetime"), formatter);
				listing.date = formatDateTime;
				
				for(Element attribute : attributes)
				{
					String [] attr = attribute.text().split(":");
					if(attr[0].toLowerCase().equals("transmission"))
					{
						listing.attr_transmission = attr[1].trim();
					}
					if(attr[0].toLowerCase().equals("title status"))
					{
						listing.attr_title_status = attr[1].trim();
					}
					if(attr[0].toLowerCase().equals("odometer"))
					{
						listing.attr_odometer = Integer.parseInt(attr[1].trim());
					}
				}
				try
				{
					listing.price = Float.parseFloat(listing_doc.select(".price").text().replace("$",""));
				}
				catch (Exception e) {
					listing.price = -1;
				}
				
				listing.num_images = listing_doc.select(".thumb").size();
				if(listing.determine_value() >= 0)
				{
					this.listings.add(listing);
				}
				
			}
			catch (Exception e) {
				// TODO: handle exception
				System.out.println("Couldn't parse listing.");
			}
			
		}
	}
	
	public boolean save_listing(Listing listing)
	{
		// TODO: try to save new listing, if it is already saved then return false
		// won't be entirely straightforward to check dups, reposts are common
		// looking through every db value would be slow, but time saving for me
		
		return true;
	}
	public void send_best() throws FileNotFoundException
	{
		
		String USER_NAME = config.get("username");  // GMail user name
	    String PASSWORD = config.get("password"); // GMail password
	    String RECIPIENT = config.get("recipient");
		
	    // make values are not null
	    // get top and format a email with links to posts
	    // profit
		
	}
	public static void main(String[] args) {
		FetchListings f = new FetchListings();
		f.get_listings("https://sfbay.craigslist.org/search/cta?sort=date&query=540i");
		try {
			f.send_best();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Integer num_listings_print = 20000;
		Integer counter = 0;
		for(Listing listing : f.listings)
		{
			counter += 1;
			if(counter > num_listings_print)
			{
				break;
			}
			System.out.println(listing.value + ": " + listing.title);
		}
    }
}
