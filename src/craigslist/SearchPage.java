package craigslist;

public class SearchPage {
	enum Category 
	{ 
	    CARS_AND_TRUCKS, BICYCLES; 
	} 
	String region;
	String query;
	Integer page;
	Category category;
	SearchPage(String region, String query, int page, Category cat)
	{
		this.region = region;
		this.query = query;
		this.page  = page;
		this.category = cat;
	}
	public String get_url()
	{
		switch(this.category) {
		case CARS_AND_TRUCKS:
			return generate_url("cta");
		case BICYCLES:
			return generate_url("bia");
		default:
			return generate_url("sss");
		}
	}
	public String generate_url(String categoryKey)
	{
		return "https://" + region + ".craigslist.org/search/" + categoryKey + "?s=" + page.toString() + "&sort=date&query=" + query;
	}
}
