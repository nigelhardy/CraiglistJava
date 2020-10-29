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
		return "Listing [" + " value=" + value + ", title=" + title + ", content=" + content + ", attr_make_model="
				+ attr_make_model + ", attr_transmission=" + attr_transmission + ", attr_odometer=" + attr_odometer
				+ ", attr_title_status=" + attr_title_status + ", price=" + price + ", num_images=" + num_images
				+ ", adType=" + adType + ", url=" + url + ", region=" + region + ", date=" + date +"]";
	}
	enum ListingType 
	{ 
	    CarOwner, CarDealer, CarParts, Other; 
	}
	ListingType adType;
	String title = "";
	String content = "";
	String attr_make_model = "";
	String attr_transmission = "";
	String attr_cylinders = "";
	String listing_url = "";
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
		listing_url = listing_doc.baseUri();
		String region = listing_url.split("//")[1].split("\\.")[0];
		this.region = region;
		
		if(listing_url.contains("/cto/"))
		{
			adType = ListingType.CarOwner;
		}
		else if(listing_url.contains("/pts/"))
		{
			adType = ListingType.CarParts;
		}
		else if(listing_url.contains("/ctd/"))
		{
			adType = ListingType.CarDealer;
		}
		else
		{
			adType = ListingType.Other;
		}
		
		if(title.size() > 0)
		{
			try 
			{
				this.title = title.text().toLowerCase();
				this.content = content.text().replace(content.select(".print-qrcode-label").text(), "").trim().toLowerCase();
				Elements attributes = listing_doc.select("p.attrgroup:gt(0)").select("span");
				if(listing_url.contains("/cta/") || listing_url.contains("/ctd/"))
				{
					this.attr_make_model = listing_doc.select("p.attrgroup span").get(0).text();
				}
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
					if(attr[0].toLowerCase().equals("cylinders"))
					{
						this.attr_cylinders = attr[1].trim();
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
//				if(this.determine_value() >= 0)
//				{
//					//save_listing(this);
//				}
				
			}
			catch (Exception e) {
				e.printStackTrace();
				System.out.println("Couldn't parse listing.");
			}
			
		}
	}
	public float fake_value(int i)
	{
		value = (float) i;
		return value;
	}
	public void changeValue(ArrayList<String> keys, Float points, String text)
	{
		for(String key : keys)
		{
			changeValue(key, points, text);
		}
	}
	public void changeValue(String[] keys, Float points, String text)
	{
		for(String key : keys)
		{
			changeValue(key, points, text);
		}
	}
	public void changeValue(String key, Float points, String text)
	{
		if(text.contains(key))
		{
			value += points;
		}
	}
	public Boolean containsKeys(String[] keys, String text)
	{
		for(String key : keys)
		{
			if(text.contains(key))
			{
				return true;
			}
		}
		return false;
	}
	public Boolean containsKeys(ArrayList<String> keys, String text)
	{
		for(String key : keys)
		{
			if(text.contains(key))
			{
				return true;
			}
		}
		return false;
	}
	public void disqualify(String[] keys, String text) throws Exception
	{
		for(String key : keys)
		{
			disqualify(key, text);
		}
	}
	public void disqualify(ArrayList<String> keys, String text) throws Exception
	{
		for(String key : keys)
		{
			disqualify(key, text);
		}
	}
	public void disqualify(String key, String text) throws Exception
	{
		if(text.contains(key))
		{
			value = -1f;
			throw new Exception("Disqualify Listing!");
		}
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
	public float set_m62_value()
	{
		value = 0f;
		try {
			content = content.split("keyword")[0];
			Boolean already_categorized = false;
			String [] unwanted_models = {"audi", "honda", "subaru", "hummer", "acura",
					"gmc", "toyota", "kia", "scion", "lincoln", "ford", "mini cooper",
					"buick", "chrysler", "nissan", "mercedes", "r350", "dodge ram", "brz", 
					"challenger", "chevrolet", "cadillac", "porsche", "forester", "mini clubman",
					"cooper", "lexus", "chevy", "chevrolet", "volvo", "kubota", "volkswagen",
					"fiat", "infiniti", "hyundai", "suzuki", "jeep", "dodge",
					"e90", "3 series", "x6", "mazda", "bentley", "polaris",
					"yamaha", "sprinter", "tractor", "maserati", "lr3", "jaguar",
					"discovery", "335i", "mustang", "lamborghini", "z4", "plymouth",
					"745i", "freelander", "supercharged", "545i", "535i", "330i", "745li",
					"z3"};
			disqualify(unwanted_models, title);
			disqualify(unwanted_models, attr_make_model);
			
			String [] goodstuff = {"parts car", "parting out", "for parts", "collision", "m62b44", "m62tu", "new engine",
					"4.4", "v8", "v-8", "runs strong", "runs good", "bad transmission", "mechanic", "special", "running",
					"chain guides", "timing chain", "engine good", "pulls hard", "damage", "crash", "salvage"};
			changeValue(goodstuff, 4f, content);

			if(adType == ListingType.CarOwner)
			{
				value += 3f;
			}
			else if(adType == ListingType.CarParts)
			{
				value += 1;
			}
			else if(adType == ListingType.CarDealer)
			{
				value -= 1f;
			}
			else if(adType == ListingType.Other)
			{

			}
			if(this.price > 12000)
				value -= 5f;
			if(this.price > 7000)
				value -= 3f;
			else if(this.price > 5000)
				value -= 1.5f;
			else if(this.price < 3000)
				value += 1f;
			if(title.contains("e46"))
			{
				value -= 3f;
			}
			
			if(title.contains("parts/service/mods by a true bmw enthusiast") ||
					title.contains("mobile programming diagnose") ||
					title.contains("mobile programming diagnose"))
			{
				throw new Exception("Part/service/mods guys must go!");
			}

			ArrayList<String> all_avoid_years = new ArrayList<String>();
			for (Integer i = 2007; i <= 2020; i++) {
				all_avoid_years.add(i.toString());
			}
			for (Integer i = 1980; i <= 1994; i++) {
				all_avoid_years.add(i.toString());
			}
			ArrayList<String> avoid_7s_years = new ArrayList<String>();
			for (Integer i = 1990; i <= 1998; i++) {
				avoid_7s_years.add(i.toString());
			}
			for (Integer i = 2002; i <= 2020; i++) {
				avoid_7s_years.add(i.toString());
			}
			avoid_7s_years.add("95 ");
			avoid_7s_years.add("96 ");
			avoid_7s_years.add("97 ");
			avoid_7s_years.add("98 ");
			ArrayList<String> good_7s_years = new ArrayList<String>();
			for (Integer i = 1999; i <= 2001; i++) {
				good_7s_years.add(i.toString());
			}
			good_7s_years.add("99 ");
			good_7s_years.add("00 ");
			good_7s_years.add("01 ");

			disqualify(all_avoid_years, title);
			disqualify(all_avoid_years, attr_make_model);
			if(attr_odometer > 200000)
			{
				throw new Exception("Mileage too high!");
			}
			else if(attr_odometer > 150000)
			{
				value -= 2f;
			}
			else if(attr_odometer > 130000)
			{
				value += 1.f;
			}
			else if(attr_odometer < 100000)
			{
				value += 1f;
			}

			/////////////////////////////////740
			String[] sevenSeries = {"740", "e38", "7 series"};
			if(containsKeys(sevenSeries, title) || containsKeys(sevenSeries, attr_make_model))
			{

				already_categorized = true;
				value += 1f;
				Boolean good_year_found = false;
				for(String year : good_7s_years)
				{
					if(title.contains(year) || attr_make_model.contains(year))
					{
						if((containsKeys(good_7s_years, title) && containsKeys(sevenSeries, title)) ||
								(containsKeys(good_7s_years, attr_make_model) && containsKeys(sevenSeries, title)))
						{
							value += 5f;
						}
						value += 3f;
						good_year_found = true;
						break;
					}
				}
				for(String year : avoid_7s_years)
				{
					if(title.contains(year) || attr_make_model.contains(year))
					{
						if(!good_year_found)
						{
							throw new Exception("740 with bad year, and no good year");
						}
					}
				}
			}
			String[] fiveSeries = {"540", "e39", "5 series"};
			ArrayList<String> good_5s_years = new ArrayList<String>();
			for (Integer i = 1999; i <= 2003; i++) {
				good_5s_years.add(i.toString());
			}
			good_5s_years.add("99 ");
			good_5s_years.add("00 ");
			good_5s_years.add("01 ");
			good_5s_years.add("02 ");
			good_5s_years.add("03 ");
			ArrayList<String> bad_5s_years = new ArrayList<String>();
			for (Integer i = 1985; i <= 1998; i++) {
				bad_5s_years.add(i.toString());
			}
			for (Integer i = 2004; i <= 2010; i++) {
				bad_5s_years.add(i.toString());
			}
			bad_5s_years.add("95 ");
			bad_5s_years.add("96 ");
			bad_5s_years.add("97 ");
			bad_5s_years.add("98 ");
			String[] closeButNotOkay = {"e60", "550i", "528i", "530i", "525i", "m5", "z3", "645ci", "x3", "325i", "328i", "750i"};
			String[] acceptables = {"540", "e38", "e39", "740", "range rover", "x5", "e53"};
			if(!(containsKeys(acceptables, title) || containsKeys(acceptables, attr_make_model))
					&& (containsKeys(closeButNotOkay, title) || containsKeys(closeButNotOkay, attr_make_model)))
			{
				throw new Exception("Not a car we want, but close!");
			}
			if(containsKeys(fiveSeries, title) || containsKeys(fiveSeries, attr_make_model))
			{
				value += 1f;
				Boolean good_year_found = false;
				for(String year : good_5s_years)
				{
					if(title.contains(year) || attr_make_model.contains(year))
					{
						if((containsKeys(good_5s_years, title) && containsKeys(fiveSeries, title)) ||
								(containsKeys(good_5s_years, attr_make_model) && containsKeys(fiveSeries, title)))
						{
							value += 5f;
						}
						good_year_found = true;
						value += 3f;
						break;
					}
				}
				for(String year : bad_5s_years)
				{
					if(title.contains(year) || attr_make_model.contains(year))
					{
						if(!good_year_found)
						{
							value -= 3f;
							break;
						}
					}
				}
			}
			String[] x5model = {"x5", "e53"};
			ArrayList<String> good_x5s_years = new ArrayList<String>();
			for (Integer i = 1999; i <= 2003; i++) {
				good_x5s_years.add(i.toString());
			}
			good_x5s_years.add("01 ");
			good_x5s_years.add("02 ");
			good_x5s_years.add("03 ");

			ArrayList<String> bad_x5s_years = new ArrayList<String>();
			for (Integer i = 2004; i <= 2020; i++) {
				bad_x5s_years.add(i.toString());
			}
			bad_x5s_years.add("04 ");
			bad_x5s_years.add("05 ");
			bad_x5s_years.add("06 ");
			// NO cylinders: 6 cylinders
			String[] x5good = {"4.4", "v8", "m62"};
			String[] x5bad = {"4.8", "3.0", "6cyl", "6 cyl", "n62", "4.6"};
			
			if(containsKeys(x5model, title) || containsKeys(x5model, attr_make_model))
			{
				value += 1f;
				Boolean good_year_found = false;
				for(String year : good_x5s_years)
				{
					if(title.contains(year) || attr_make_model.contains(year))
					{
						if((containsKeys(good_x5s_years, title) && containsKeys(x5model, title)) ||
								(containsKeys(good_x5s_years, attr_make_model) && containsKeys(x5model, title)))
						{
							value += 5f;
						}
						good_year_found = true;
						value += 3f;
						break;
					}
				}
				for(String year : bad_x5s_years)
				{
					if(title.contains(year) || attr_make_model.contains(year))
					{
						if(!good_year_found)
						{
							throw new Exception("Bad years for X5!");
						}
					}
				}
				changeValue(x5bad, -3f, content);
				disqualify(x5bad, title);
				disqualify(x5bad, attr_make_model);
			}
			String[] engineKeys = {"m62"};
			if(containsKeys(engineKeys, title) || containsKeys(engineKeys, attr_make_model) ||
					containsKeys(engineKeys, content))
			{
				String[] alsoneeds = {"engine", "bmw"};
				if(containsKeys(alsoneeds, content))
				{
					value += 2f;
				}
			}
		} catch (Exception e) {
			value = -1f;
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
		if(adType != ListingType.CarOwner)
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
