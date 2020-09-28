package craigslist;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
public class Listing implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	LocalDateTime date;
	@Override
	public String toString() {
		return "Listing [date=" + date + ", title=" + title + ", content=" + content + ", attr_make_model="
				+ attr_make_model + ", attr_transmission=" + attr_transmission + ", attr_odometer=" + attr_odometer
				+ ", attr_title_status=" + attr_title_status + ", price=" + price + ", num_images=" + num_images
				+ ", by_owner=" + by_owner + ", url=" + url + ", region=" + region + ", value=" + value + "]";
	}
	String title = "";
	String content = "";
	String attr_make_model = "";
	String attr_transmission = "";
	Integer attr_odometer = -1;
	String attr_title_status = "";
	Float price = -1f;
	Integer num_images = 0;
	boolean by_owner = false;
	String url = "";
	String region = "";
	String frame_size = "";
	public Float getValue() {
		if(value == Float.NEGATIVE_INFINITY)
			set_sportwagen_value();
		return value;
	}
	static int count = 0;
	Float value = Float.NEGATIVE_INFINITY;
	public Listing()
	{
		
	}
	public Listing(Document listing_doc)
	{
		Elements title = listing_doc.select("#titletextonly");
		Elements content = listing_doc.select("#postingbody");
		String listing_url = listing_doc.baseUri();
		String region = listing_url.split("//")[1].split("\\.")[0];
		this.region = region;
		if(listing_url.contains("/cto/"))
		{
			// cto is craiglist abrev for cars/trucks by owner
			// ctd is dealership
			// cta in search is all
			this.by_owner = true;
		}
		else
		{
			this.by_owner = false;
		}
		
		if(title.size() > 0)
		{
			try 
			{
				this.title = title.text().toLowerCase();
				this.content = content.text().replace(content.select(".print-qrcode-label").text(), "").trim().toLowerCase();
				Elements attributes = listing_doc.select("p.attrgroup:gt(0)").select("span");
				this.attr_make_model = listing_doc.select("p.attrgroup span").get(0).text();
				this.url = listing_url;		
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
				LocalDateTime formatDateTime = LocalDateTime.parse(listing_doc.select(".date[datetime]").attr("datetime"), formatter);
				this.date = formatDateTime;
				
				for(Element attribute : attributes)
				{
					String [] attr = attribute.text().split(":");
					if(attr[0].toLowerCase().equals("transmission"))
					{
						this.attr_transmission = attr[1].trim();
					}
					if(attr[0].toLowerCase().equals("title status"))
					{
						this.attr_title_status = attr[1].trim();
					}
					if(attr[0].toLowerCase().equals("odometer"))
					{
						this.attr_odometer = Integer.parseInt(attr[1].trim());
					}
					if(attr[0].toLowerCase().equals("frame size"))
					{
						this.frame_size = attr[1].trim();
					}
					
				}
				try
				{
					this.price = Float.parseFloat(listing_doc.select(".price").text().replace("$","").replace(",", ""));
				}
				catch (Exception e) {
					this.price = -1f;
				}
				
				this.num_images = listing_doc.select(".thumb").size();
				if(this.determine_value() >= 0)
				{
					//save_listing(this);
				}
				
			}
			catch (Exception e) {
				System.out.println("Couldn't parse listing.");
			}
			
		}
	}
	public float fake_value(int i)
	{
		value = (float) i;
		return value;
	}
	public float set_bicycle_value()
	{
		String [] good_title= {"specialized", "gary fisher", "santa cruz", "trek", "xt", "lx", "shimano", "hardtail"};
		String [] good_content= {"specialized", "gary fisher", "santa cruz", "trek", "xt", "lx", "shimano", "large"};

		String[] bad_title = {"small", "medium", "kids", "women", "20 inch", "20 in", "20\"", "24 in", "24\""};
		String[] bad_content = {"small", "medium", "women", "kids"};
		
		for(String key : good_title) 
		{
			if(title.contains(key))
			{
				value += 1f;
			}
		}
		for(String key : good_content) 
		{
			if(content.contains(key))
			{
				value += 1f;
			}
		}
		for(String key : bad_title) 
		{
			if(content.contains(key))
			{
				value -= 1f;
			}
		}
		for(String key : bad_content) 
		{
			if(content.contains(key))
			{
				value -= 1f;
			}
		}

		if(price < 200 && price > 2)
		{
			value += 1.f;
		}
		else if(price < 300 && price > 2)
		{
			value += .5f;
		} 
		else if(price > 600)
		{
			value = -1f;
			return value;
		}
			
		return value;
	}
	public float set_sportwagen_value()
	{
		String[] models = {"sportwagen", "sportwagon", "jetta wagon", "wagon", "tdi",
				"golf wagon", "diesel"};
		
		String [] unwanted_models = {"toyota", "kia", "scion", "lincoln", "ford", "mini cooper",
				"buick", "chrysler", "nissan", "r350", "dodge ram", "brz", "mazda", 
				"challenger", "chevrolet", "x3", "porsche", "forester", "mini clubman",
				"cooper", "lexus", "chevy", "chevrolet",
				"fiat", "infiniti", "hyundai", "suzuki", "jeep", "dodge"};
		
		String [] good_keywords = {"one owner", "one-owner", "1-owner", "1 owner",
				"clean title", "low miles", "clean", "service records", "service history"};
		
		String good_title[] = {"1998", "1999", "2000", "2001", "2002", "2003", "2004", "2005"};
		
		String [] man_trans_keys = {"manual transmission", "6 speed manual", "manual", "6 speed", "6-speed",
				"6mt", "six speed", "stick shift", "manual trans", "transmission manual", "transmission: manual"};
		
		String [] man_trans_title = {"6sp", "6speed", "6 speed", "manual", "6 sp", "six speed", "6-sp",
				"6-speed", "6mt", "6m"};
		
		String [] bad_keywords = {"dsg", "salvage", "rebuilt", "sedan", "sdn", "convertible", "automatic",
				"tiptronic", "automatic transmission", "salvage title", "6a", "transmission: automatic",
				"automatic 6-speed", "6 speed auto", "auto trans", "a/t", "6-speed a/t",
				"auto 6-spd", "tptrnc", "suv", "coupe", "transmission : automatic"};
		content = content.split("keyword")[0];
		if(attr_title_status == "salvage")
		{
			value -= 1.f;
		}
		if(attr_odometer > 150000)
		{
			value -= 3f;
		}
		else if(attr_odometer > 100000)
		{
			value -= 2.f;
		}
		else if(attr_odometer > 80000)
		{
			value -= 1f;
		}
		
		if(title.contains("dsg"))
		{
			value -= 3.f;
		}
		// Remove new subarus
		for(Integer i = 2008; i <= 2020; i++)
		{
			if(title.contains(i.toString()) && title.contains("subaru"))
			{
				value = -1f;
				return value;
			}
		}
		
		for(String key : unwanted_models) 
		{
			if(title.contains(key) || attr_make_model.contains(key))
			{
				value = -1f;
				return value;
			}
		}
		for(String key : models) 
		{
			if(title.contains(key) || attr_make_model.contains(key))
				value += 2f;
		}
		for(String key : good_title) 
		{
			if(title.contains(key) || attr_make_model.contains(key))
			{
				value += 1f;
				break;
			}
		}
		for(String key : man_trans_keys) 
		{
			if(title.contains(key) || attr_make_model.contains(key))
			{
				value += 1f;
				break;
			}
		}
		for(String key : man_trans_keys) 
		{
			if(content.contains(key))
			{
				value += .5f;
				break;
			}
		}
		for(String key : good_keywords) 
		{
			if(content.contains(key))
				value += 1.f;
		}
		float bad_key_val = 0f;
		for(String key : bad_keywords) 
		{
			if(content.contains(key))
				bad_key_val -= 1.f;
			if(bad_key_val < -2)
			{
				break;
			}
		}
		value += bad_key_val;
		bad_key_val = 0f;
		for(String key : bad_keywords) 
		{
			if(title.contains(key) || attr_make_model.contains(key))
				bad_key_val -= 1.f;
			if(bad_key_val < -2)
			{
				break;
			}
		}
		value += bad_key_val;
		if(attr_transmission.contains("automatic"))
			value -= 2f;
		if(price < 10000 && price > 1001)
		{
			value += 1.f;
		}
		if(price > 14000)
		{
			value = -1.f;
			return value;
		}
		return value;
	}
	public float set_miata_value()
	{
		String[] models = {"miata", "mx5", "mx-5", "mx 5"};
		
		String [] unwanted_models = {"toyota", "kia", "scion", "lincoln", "ford", "mini cooper",
				"buick", "chrysler", "nissan", "r350", "dodge ram", "brz", 
				"challenger", "chevrolet", "x3", "porsche", "forester", "mini clubman",
				"cooper", "lexus", "chevy", "chevrolet",
				"fiat", "infiniti", "hyundai", "suzuki", "jeep", "dodge"};
		
		String [] good_keywords = {"one owner", "one-owner", "1-owner", "1 owner",
				"clean title", "low miles", "clean", "service records", "service history"};
		
		String good_title[] = {"2010", "2011", "2012", "2013", "2014"};
		
		ArrayList<String> dq_title = new ArrayList<String>();
		for(Integer year = 2010; year <= 2020; year++)
		{
			dq_title.add(year.toString());
		}
		
		String [] man_trans_keys = {"manual transmission", "6 speed manual", "manual", "6 speed", "6-speed",
				"6mt", "six speed", "stick shift", "manual trans", "transmission manual", "transmission: manual"};
		
		String [] man_trans_title = {"6sp", "6speed", "6 speed", "manual", "6 sp", "six speed", "6-sp",
				"6-speed", "6mt", "6m"};
		
		String [] bad_keywords = {"dsg", "salvage", "rebuilt", "sedan", "sdn", "automatic",
				"tiptronic", "automatic transmission", "salvage title", "6a", "transmission: automatic",
				"automatic 6-speed", "6 speed auto", "auto trans", "a/t", "6-speed a/t",
				"auto 6-spd", "tptrnc", "suv", "coupe", "transmission : automatic"};
		content = content.split("keyword")[0];
		
		if(attr_transmission.contains("automatic"))
			value -= 2f;
		if(!by_owner)
			value -= 1.5f;
		if(attr_title_status == "salvage")
		{
			value -= 1.f;
		}
		if(attr_odometer > 200000)
		{
			value = -1f;
			return -1f;
		}
		else if(attr_odometer > 150000)
		{
			value -= 3f;
		}
		else if(attr_odometer > 130000)
		{
			value -= 1.f;
		}
		else if(attr_odometer < 100000)
		{
			value += 1f;
		}
		for(String dq : dq_title)
		{
			if(title.contains(dq))
			{
				value = -1f;
				return -1;
			}
		}
		if(title.contains("dsg"))
		{
			value -= 3.f;
		}
		// Remove new subarus
		for(Integer i = 2008; i < 2020; i++)
		{
			if(title.contains(i.toString()) && title.contains("subaru"))
			{
				value = -1f;
				return value;
			}
		}
		boolean model_match = false;
		for(String key : models) 
		{
			if(title.contains(key) || attr_make_model.contains(key))
			{
				model_match = true;
			}
		}
		if(!model_match)
		{
			value = -1f;
			return -1f;
		}
		for(String key : unwanted_models) 
		{
			if(title.contains(key) || attr_make_model.contains(key))
			{
				value = -1f;
				return value;
			}
		}
		for(String key : models) 
		{
			if(title.contains(key) || attr_make_model.contains(key))
				value += 2f;
		}
		for(String key : good_title) 
		{
			if(title.contains(key) || attr_make_model.contains(key))
			{
				value += 1f;
				break;
			}
		}
		for(String key : man_trans_keys) 
		{
			if(title.contains(key) || attr_make_model.contains(key))
			{
				value += 1f;
				break;
			}
		}
		for(String key : man_trans_keys) 
		{
			if(content.contains(key))
			{
				value += .5f;
				break;
			}
		}
		for(String key : good_keywords) 
		{
			if(content.contains(key))
				value += 1.f;
		}
		float bad_key_val = 0f;
		for(String key : bad_keywords) 
		{
			if(content.contains(key))
				bad_key_val -= 1.f;
			if(bad_key_val < -2)
			{
				break;
			}
		}
		value += bad_key_val;
		bad_key_val = 0f;
		for(String key : bad_keywords) 
		{
			if(title.contains(key) || attr_make_model.contains(key))
				bad_key_val -= 1.f;
			if(bad_key_val < -2)
			{
				break;
			}
		}
		value += bad_key_val;
		if(attr_transmission.contains("automatic"))
			value -= 2f;
		if(price < 5000 && price > 1001)
		{
			value += 1.f;
		}
		if(price > 9000)
		{
			value = -1.f;
			return value;
		}
		return value;
	}
	// Nigel's legacy function for 540i
	public float determine_value()
	{
		String model = "540";
		
		// disqualified if string found in the title
		String [] title_dq = {"540ia", "automatic", "auto", "mechanic special", "parts", "part out", "part-out"};
		// also disqualified if found in the title
		String [] unwanted_models = {"325i", "328i", "330i", "525i", "530i", "535i", "550i", "740i", "X3", "X5", "X7",
				"mercedes", "acura", "nissan", "volkswagen", "honda", "toyota", "lexus", "audi", "scion", "porsche"};
		// disqualified if found in the body/content
		String [] content_dq = {"540ia", "auto transmission", "automatic transmission", "auto trans", "parting out", "not running"};
		
		// select range of desired model years
		int min_year_desired = 2000;
		int max_year_desired = 2003;
		
		// avoid these model years (except for desired)
		int min_year_avoid = 1990;
		int max_year_avoid = 2018;
				
		// Going to make this dirty and not very dynamic, don't judge
		String [] man_trans_keys = {"manual transmission", "manual", "6 speed", "6-speed", "6 speed transmission", "six speed"};
		String [] msport_keys = {"m sport", "m-sport", "msport", "sport"};
		
		// minus points
		String [] bad = {"salvage title", "salvage", "oil leak", "leaking", "needs smog", "rebuild", 
				"cracked windshield", "as is", "needs work", "needs tlc", "non op"};
		
		if(attr_transmission.equals("automatic"))
		{
			value = -1f;
			return value;
		}
		if(attr_odometer > 150000)
		{
			value = -1f;
			return value;
		}
		else if(attr_odometer > 100000)
		{
			value -= .5f;
		}
		
		if(!title.contains(model) && !attr_make_model.contains(model))
		{
			// doesn't have model, so lets get rid of it
			value = -1f;
			return value;
		}
		for(String key : title_dq) 
		{
			if(title.contains(key) || attr_make_model.contains(key))
			{
				value = -1f;
				return value;
			}
		}
		for(String key : unwanted_models) 
		{
			if(title.contains(key) || attr_make_model.contains(key))
			{
				value = -1f;
				return value;
			}
		}
		for(String key : content_dq) 
		{
			if(title.contains(key) || content.contains(key) || attr_make_model.contains(key))
			{
				value = -1f;
				return value;
			}
		}
		for(Integer i = min_year_avoid; i <= max_year_avoid; i++)
		{
			if(i <= max_year_desired && i >= min_year_desired)
			{
				continue;
			}
			if(title.contains(i.toString()) || attr_make_model.contains(i.toString()))
			{
				value = -1f;
				return value;
			}
			Integer short_year = i;
			if(i >= 2000)
			{
				short_year -= 2000;
			}
			else
			{
				short_year -= 1900;
			}
			String short_year_str = short_year.toString();
			if(short_year_str.length() < 2)
			{
				short_year_str = "0" + short_year_str;
			}
			if(title.contains(short_year_str) || attr_make_model.contains(short_year_str))
			{
				value = -1f;
				break;
			}
		}
		for(Integer i = min_year_desired; i <= max_year_desired; i++)
		{
			if(title.contains(i.toString()) || attr_make_model.contains(i.toString()))
			{
				// contains a desired year
				value += 2;
				break;
			}
			// weird hacky way to check for '03 or 99 (short version of year)
			Integer short_year = i;
			if(i >= 2000)
			{
				short_year -= 2000;
			}
			else
			{
				short_year -= 1900;
			}
			String short_year_str = short_year.toString();
			if(short_year_str.length() < 2)
			{
				short_year_str = "0" + short_year_str;
			}
			if(title.contains(short_year_str) || attr_make_model.contains(short_year_str))
			{
				value += 2;
				break;
			}
		}
		for(String key : man_trans_keys) 
		{
			if(title.contains(key) || content.contains(key))
			{
				value += 2;
				break;
			}
		}
		for(String key : msport_keys) 
		{
			if(title.contains(key) || content.contains(key))
			{
				value += 2;
				break;
			}
		}
		for(String key : bad) 
		{
			if(title.contains(key) || content.contains(key))
			{
				value -= .5f;
			}
		}
		if(attr_title_status.equals("salvage"))
		{
			value -= .5f;
		}
		return value;
	}
}
