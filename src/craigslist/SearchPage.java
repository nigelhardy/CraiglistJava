package craigslist;

public class SearchPage {
	String region;
	String query;
	Integer page;
	SearchPage(String region, String query, int page)
	{
		this.region = region;
		this.query = query;
		this.page  = page;
	}
	public String get_url()
	{
		return "https://" + region + ".craigslist.org/search/cta?s=" + page.toString() + "&sort=date&query=" + query;
	}
}
