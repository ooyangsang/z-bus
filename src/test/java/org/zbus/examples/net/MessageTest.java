package org.zbus.examples.net;

import org.zbus.net.http.Message;

public class MessageTest {

	public static void main(String[] args) throws Exception {
		Message msg = new Message();
		msg.setBody("hello"); 
		
		System.out.println(new String(msg.toBytes()));
		 
		System.err.println(Message.parse(msg.toBytes()));
	}

}
