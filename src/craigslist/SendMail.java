package craigslist;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import javax.mail.*;
import javax.mail.internet.*;



public class SendMail {
	Map<String, String> config = new HashMap<String, String>();
	
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
	public void send_notification(String subject, String body)
	{
		
		String USER_NAME = config.get("username");  // GMail user name
	    String PASSWORD = config.get("password"); // GMail password
	    String[] RECIPIENT = {config.get("recipient")};
		
	    if(USER_NAME != null && PASSWORD != null && RECIPIENT != null)
	    {
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
}
