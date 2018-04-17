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

import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.text.similarity.*;

public class FetchListings {
	public static int SIM_THRESH = 10;
	ListingDB db;
	Vector<Listing> listings = new Vector<Listing>();
	Vector<Listing> alert_listings = new Vector<Listing>();
	Map<String, String> config = new HashMap<String, String>();
	LevenshteinDistance lev_dist = null;
	public FetchListings()
	{
		read_config();
		db = new ListingDB();
		lev_dist = new LevenshteinDistance(100);
	}
	public void read_config()
	{
		// read config
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
			e.printStackTrace();
			System.out.println("Could not read config file.");
		}
	}
	public void get_listings(String region, Integer page, String query)
	{
		String url = "https://" + region + ".craigslist.org/search/cta?s=" + page.toString() + "&sort=date&query=" + query;
		System.out.println("Getting " + url);
		try {
			File search_page = new File("test_pages/" + region + page.toString());
			Document doc = null;
			if(search_page.exists())
			{
				doc = Jsoup.parse(search_page, "UTF-8");
			}
			else
			{
				doc = Jsoup.connect(url).get();
				downloadPageSearch(doc, url, region, page);
			}
			Elements results = doc.select("p.result-info");
			for (Element p_elem : results) 
	        {
				Elements res_links = p_elem.select("a.result-title");
				
				for(Element a_elem : res_links) 
				{
					String listing_url = a_elem.attr("href");
					Document listing_doc = null;
					String[] url_parts = listing_url.split("/");
					File listing_page = new File("test_pages/" + url_parts[url_parts.length-1]);
					if(listing_page.exists())
					{
						listing_doc = Jsoup.parse(listing_page, "UTF-8");
					}
					else
					{
						listing_doc = Jsoup.connect(listing_url).get();
						downloadPage(listing_doc, listing_url);
					}
					create_listing(listing_doc, listing_url, region);
					
				}				
	        }
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Exception when traversing url: " + url);
		}
	}
	public void create_listing(Document listing_doc, String listing_url, String region)
	{
		Elements title = listing_doc.select("#titletextonly");
		Elements content = listing_doc.select("#postingbody");
		Listing listing = new Listing();
		listing.region = region;
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
				listing.attr_make_model = listing_doc.select("p.attrgroup span").get(0).text();
				listing.url = listing_url;		
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
					listing.price = -1f;
				}
				
				listing.num_images = listing_doc.select(".thumb").size();
				if(listing.determine_value() >= 0)
				{
					save_listing(listing);
				}
				
			}
			catch (Exception e) {
				System.out.println("Couldn't parse listing.");
			}
			
		}
	}
	
	public boolean save_listing(Listing new_listing)
	{
		for(Listing listing : listings)
		{
			int temp_ld = lev_dist.apply(new_listing.content, listing.content);
			if(temp_ld < SIM_THRESH && temp_ld != -1)
			{
				return false;
			}
		}
		db.save_listing(new_listing);
		listings.add(new_listing);
		return true;
	}
	public void send_best()
	{
		
		String USER_NAME = config.get("username");  // GMail user name
	    String PASSWORD = config.get("password"); // GMail password
	    String[] RECIPIENT = {config.get("recipient")};
		
	    if(USER_NAME != null && PASSWORD != null && RECIPIENT != null)
	    {
	    	String subject = "Found new 540i posting(s) on Craigslist";
	        String body = "";
	        for(Listing listing : listings)
	        {
	        	body += listing.value + ": " + listing.title + " - " + listing.url;
	        	body += "\n";
	        }
	        sendFromGMail(USER_NAME, PASSWORD, RECIPIENT, subject, body);
	    }
	    else
	    {
	    	System.out.println("Need to have config variables set to send email.");
	    }
		
	}
	
	private static void sendFromGMail(String from, String pass, String[] to, String subject, String body) {
		// credit goes to Bill the Lizard
		// https://stackoverflow.com/questions/46663/how-can-i-send-an-email-by-java-application-using-gmail-yahoo-or-hotmail
        Properties props = System.getProperties();
        String host = "smtp.gmail.com";
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.user", from);
        props.put("mail.smtp.password", pass);
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");

        Session session = Session.getDefaultInstance(props);
        MimeMessage message = new MimeMessage(session);

        try {
            message.setFrom(new InternetAddress(from));
            InternetAddress[] toAddress = new InternetAddress[to.length];

            // To get the array of addresses
            for( int i = 0; i < to.length; i++ ) {
                toAddress[i] = new InternetAddress(to[i]);
            }

            for( int i = 0; i < toAddress.length; i++) {
                message.addRecipient(Message.RecipientType.TO, toAddress[i]);
            }

            message.setSubject(subject);
            message.setText(body);
            Transport transport = session.getTransport("smtp");
            transport.connect(host, from, pass);
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();
        }
        catch (AddressException ae) {
            ae.printStackTrace();
        }
        catch (MessagingException me) {
            me.printStackTrace();
        }
    }
	public void print_listings(int max)
	{
		Integer num_listings_print = max;
		Integer counter = 0;
		for(Listing listing : listings)
		{
			counter += 1;
			if(counter > num_listings_print)
			{
				break;
			}
			System.out.println(listing.value + ": " + listing.title + " - " + listing.region);
		}
	}
	public void downloadPage(Document doc, String url) throws Exception {
		String[] url_parts = url.split("/");
        final File f = new File("test_pages/" + url_parts[url_parts.length-1]);
        FileUtils.writeStringToFile(f, doc.outerHtml(), "UTF-8");
    }
	public void downloadPageSearch(Document doc, String url, String region, Integer page) throws Exception {
        final File f = new File("test_pages/" + region + page.toString());
        FileUtils.writeStringToFile(f, doc.outerHtml(), "UTF-8");
    }
	public static void get_all_listings()
	{
		String[] regions = {"monterey", "losangeles", "sfbay", "santabarbara", "orangecounty", "sacramento"};
		String[] queries = {"540i"};
		FetchListings f = new FetchListings();
		int num_pages = 3;
		for(String region : regions)
		{
			for(String query : queries)
			{
				for(int i = 0; i < num_pages; i++)
				{
					f.get_listings(region, i, query);
				}
			}
		}
		
		//f.send_best();
		f.print_listings(100);
		
	}
	public static void main(String[] args) {
		get_all_listings();
    }
}
