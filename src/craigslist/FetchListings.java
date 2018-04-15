package craigslist;

import java.io.IOException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class FetchListings {
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
				break;
	        }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void create_listing(Document listing_doc)
	{
		Elements title = listing_doc.select("#titletextonly");
		Elements content = listing_doc.select("#postingbody");
		Listing listing = new Listing();
		if(title.size() > 0)
		{
			listing.title = title.text();
			String content_filtered = content.text().replace(content.select(".print-qrcode-label").text(), "").trim();
			listing.content = content_filtered;
			listing.price = Float.parseFloat(listing_doc.select(".price").text().replace("$",""));
			System.out.println(Float.parseFloat(listing_doc.select(".price").text().replace("$","")));
		}
	}
	
	
	public static void main(String[] args) {
		FetchListings f = new FetchListings();
		f.get_listings("https://sfbay.craigslist.org/search/sss?sort=date&query=540i");
    }
}
