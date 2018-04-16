package craigslist;

import java.util.Date;

public class Listing {
	String title;
	String content;
	String attr_transmission;
	String attr_title_status;
	Integer attr_odometer;
	Date date;
	Integer num_images;
	float price;
	float value = 0;
	
	public void determine_value()
	{
		// Going to make this dirty and not very dynamic, don't judge
		String [] good = {"540i", "540 ", "e39", "manual Transmission", "manual", "6 speed", "6 speed transmission",
				"low miles", "m sport", "m-sport", "msport", "2001", "2002", "2003"};
		String [] bad = {"540ia", "auto Transmission", "automatic transmission", "auto trans", "auto", "automatic", "salvage title", "salvage"};
		String [] unwanted_models = {"525i", "330i", "740i", "740il", "325i", 
				"mercedes", "acura", "nissan", "volkswagen", "535i", "X3", "X5", "X7", "civic", "honda", "altima",
				"1992", "1993", "1994", "1995", "1996", "1997", "1998", 
				"2004", "2005", "2006", "2007", "2008", "2009", "2010", "2011", "2012", "2013", "2014"};
		
		for(String key : good) 
		{
			if(content.contains(key))
			{
				value += 1;
			}
			if(title.contains(key))
			{
				value += 2;
			}
		}
		for(String key : bad) 
		{
			if(content.contains(key))
			{
				value -= 1;
			}
			if(title.contains(key))
			{
				value -= 2;
			}
		}
		for(String key : unwanted_models) 
		{
			if(content.contains(key))
			{
				value -= 1;
			}
			if(title.contains(key))
			{
				value -= 2;
			}
		}
	}
}
