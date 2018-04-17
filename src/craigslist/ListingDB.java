package craigslist;

import java.sql.*;
import org.apache.commons.text.similarity.*;

public class ListingDB {
	private Connection c = null;
	LevenshteinDistance lev_dist = null;
	public ListingDB()
	{	      
		lev_dist = new LevenshteinDistance(100);
		try {
			Class.forName("org.sqlite.JDBC");
			c = DriverManager.getConnection("jdbc:sqlite:sqlite/listing.db");
			create_listing_table();
	  	} catch ( Exception e ) {
			System.err.println( e.getClass().getName() + ": " + e.getMessage() );
			System.exit(0);
	  	}
		System.out.println("Opened database successfully");
	}
	public void create_listing_table() throws SQLException
	{
		Statement stmt = c.createStatement();
		String sql = "CREATE TABLE IF NOT EXISTS 'Listings' (" +
						"'id'					INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
						"'date'					TEXT," +
						"'title'				TEXT," +
						"'content'				TEXT," +
						"'attr_make_model'		TEXT," +
						"'attr_transmission'	TEXT," +
						"'attr_odometer'		INTEGER," +
						"'attr_title_status'	TEXT," +
						"'price'				NUMERIC," +
						"'num_images'			INTEGER," +
						"'by_owner'				INTEGER," +
						"'url'					TEXT," +
						"'region'				TEXT," +
						"'value'				NUMERIC" + 
						");";
		stmt.executeUpdate(sql);
		stmt.close();
	}
	public synchronized boolean save_listing(Listing listing)
	{
		try
		{
			if(!is_unique(listing))
			{
				return false;
			}
			String sql = "INSERT INTO 'Listings'('date','title','content'," + 
					"'attr_make_model','attr_transmission','attr_odometer','attr_title_status'," + 
					"'price','num_images','by_owner','url','region','value')" + 
					"VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?);";
			
			PreparedStatement stmt = c.prepareStatement(sql);
			stmt.setString(1, listing.date.toString());
			stmt.setString(2, listing.title);
			stmt.setString(3, listing.content);
			stmt.setString(4, listing.attr_make_model);
			stmt.setString(5, listing.attr_transmission);
			stmt.setInt(6, listing.attr_odometer);
			stmt.setString(7, listing.attr_title_status);
			stmt.setFloat(8, listing.price);
			stmt.setInt(9, listing.num_images);
			if(listing.by_owner) { stmt.setInt(10, 1); } else { stmt.setInt(10, 0); };
			stmt.setString(11, listing.url);
			stmt.setString(12, listing.region);
			stmt.setFloat(13, listing.value);
			stmt.executeUpdate();
			stmt.close();
			return true;
		}
		catch (SQLException e) {
			e.printStackTrace();
			System.out.println("Error adding listing to db");
		}
		
		return false;
	}
	public boolean is_unique(Listing listing) throws SQLException
	{
		String sql = "SELECT * FROM Listings WHERE title LIKE ?";
		PreparedStatement stmt = c.prepareStatement(sql);
		stmt.setString(1, listing.title);
		ResultSet rs = stmt.executeQuery();
		while(rs.next()){
			String content = rs.getString("content");
			int temp_ld = lev_dist.apply(content, listing.content);
			if(temp_ld != -1 && (temp_ld < 8 && content.length() > 100) || (temp_ld < 3 && content.length() < 100))
			{
				return false;
			}
		}
		return true;
	}
}
