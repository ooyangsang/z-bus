package org.zbus.examples.net;

import org.zbus.kit.ConfigKit;
import org.zbus.net.Server;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageProcessor;
import org.zbus.net.http.MessageAdaptor;
import org.zbus.net.http.MessageServer;

public class MessageServerExample {
	
	public static void main(String[] args) throws Exception {  
		int port = ConfigKit.option(args, "-p", 8080);  
		 
		Server server = new MessageServer();
		
		try {
			MessageAdaptor ioAdaptor = new MessageAdaptor(); 
			ioAdaptor.url("/", new MessageProcessor() { 
				@Override
				public Message process(Message request) { 
					System.out.println(request);
					Message res = new Message();
					res.setStatus(200);
					res.setBody("hello: " + System.currentTimeMillis());
					return res;
				}
			});   
			
			server.start(port, ioAdaptor);  
			server.join();
		} finally { 
			server.close();
		} 
	}

}
