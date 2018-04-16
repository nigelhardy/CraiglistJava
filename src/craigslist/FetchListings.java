package craigslist;

import java.util.Date;
import java.util.Vector;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class FetchListings {
	Vector<Listing> listings = new Vector<Listing>();
	Vector<Listing> alert_listings = new Vector<Listing>();
	float threshold = 1;
	
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
					Document listing_doc = Jsoup.connect(a_elem.attr("href")).get();
					create_listing(listing_doc);
					
				}
	        }
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println(url);
		}
	}
	public void create_listing(Document listing_doc)
	{
		Elements title = listing_doc.select("#titletextonly");
		Elements content = listing_doc.select("#postingbody");
		Listing listing = new Listing();
		if(title.size() > 0)
		{
			try 
			{
				// Would be nice to save private dealer vs. stealership
				// should make date actual date posted on craiglist, not retrieved (maybe have both)
				listing.title = title.text().toLowerCase();
				listing.content = content.text().replace(content.select(".print-qrcode-label").text(), "").trim().toLowerCase();
				Elements attributes = listing_doc.select("p.attrgroup").select("span");
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
				
				listing.date = new Date();
				listing.num_images = listing_doc.select(".thumb").size();
				this.listings.add(listing);
			}
			catch (Exception e) {
				// TODO: handle exception
				System.out.println("Couldn't parse listing.");
			}
			
		}
	}
	public void rank_listings()
	{
		for(Listing listing : listings)
		{
			listing.determine_value();
			if(listing.value > threshold)
			{
				if(save_listing(listing))
				{
					alert_listings.add(listing);
				}
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
	public void send_best()
	{
		// TODO: send email with best listings
	}
	public static void main(String[] args) {
		FetchListings f = new FetchListings();
		f.get_listings("https://sfbay.craigslist.org/search/cta?sort=date&query=540i");
		f.rank_listings();
		f.send_best();
		
		Integer num_listings_print = 20000;
		Integer counter = 0;
		for(Listing listing : f.alert_listings)
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
