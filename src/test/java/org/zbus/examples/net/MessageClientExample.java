package org.zbus.examples.net;

import org.zbus.kit.ConfigKit;
import org.zbus.net.EventDriver;
import org.zbus.net.http.Message;
import org.zbus.net.http.MessageClient;

public class MessageClientExample { 
 
	public static void main(String[] args) throws Exception {
		String address = ConfigKit.option(args, "-h", "127.0.0.1:8080");
		 
		EventDriver driver = new EventDriver();
		
		MessageClient client = new MessageClient(address, driver); 
		
		try { 
			Message msg = new Message();
			msg.setUrl("/");
			
			Message res = client.invokeSync(msg);
			System.out.println(res);
			
			client.close();  
		} finally {
			driver.close(); 
		}

		System.out.println("---done---");
	}
}
