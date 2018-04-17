package craigslist;

import java.time.LocalDateTime;

public class Listing {
	LocalDateTime date;
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
	Float value = 0.0f;
	
	public float determine_value()
	{
		String model = "540";
		
		
		String [] title_dq = {"540ia", "automatic", "auto", "mechanic special", "parts"};
		String [] unwanted_models = {"325i", "328i", "330i", "525i", "530i", "535i", "550i", "740i", "X3", "X5", "X7",
				"mercedes", "acura", "nissan", "volkswagen", "honda", "toyota", "lexus", "audi", "scion", "porsche"};
		String [] content_dq = {"540ia", "auto transmission", "automatic transmission", "auto trans", "parting out", "not running"};
		
		int min_year_desired = 2000;
		int max_year_desired = 2003;
		
		int min_year_avoid = 1990;
		int max_year_avoid = 2018;
		
		String[] short_years_desired = {"99", "00", "01", "02", "03"};
		
		// Going to make this dirty and not very dynamic, don't judge
		String [] man_trans_keys = {"manual transmission", "manual", "6 speed", "6-speed", "6 speed transmission", "six speed"};
		String [] msport_keys = {"m sport", "m-sport", "msport", "sport"};
		
		String [] bad = {"salvage title", "salvage", "oil leak", "leaking", "needs smog", "rebuild", "cracked windshield"};
		
		if(attr_transmission.equals("automatic"))
		{
			value = -1f;
			return value;
		}
		
		if(!title.contains(model) && !attr_make_model.contains(model))
		{
			// doesn't have 540, so lets get rid of it
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
				value += 1;
				break;
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
				value += 1;
				break;
			}
		}
		for(String key : man_trans_keys) 
		{
			if(title.contains(key) || content.contains(key))
			{
				value += 1;
				break;
			}
		}
		for(String key : msport_keys) 
		{
			if(title.contains(key) || content.contains(key))
			{
				value += 1;
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
		return value;
	}
}
