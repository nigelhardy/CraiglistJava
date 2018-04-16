package craigslist;

import java.util.ArrayList;
import java.util.Date;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class FetchListings {
	ArrayList<Listing> listings = new ArrayList<Listing>();
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
				listing.title = title.text();
				listing.content = content.text().replace(content.select(".print-qrcode-label").text(), "").trim();
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
		
	}
	
	public void save_listings()
	{
		
	}
	public void filter_old()
	{
		
	}
	public void send_best()
	{
		
	}
	public static void main(String[] args) {
		FetchListings f = new FetchListings();
		f.get_listings("https://sfbay.craigslist.org/search/cta?sort=date&query=540i");
		f.rank_listings();
		f.filter_old();
		f.send_best();
		f.save_listings();
		for(Listing listing : f.listings)
		{
			System.out.println(listing.attr_odometer);
		}
    }
}
