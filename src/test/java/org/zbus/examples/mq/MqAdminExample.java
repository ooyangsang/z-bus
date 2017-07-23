package org.zbus.examples.mq;

import org.zbus.broker.Broker;
import org.zbus.broker.ZbusBroker;
import org.zbus.mq.MqAdmin;

public class MqAdminExample { 
	public static void main(String[] args) throws Exception {  
		Broker broker = new ZbusBroker("127.0.0.1:15555");   
		MqAdmin admin = new MqAdmin(broker, "MyMQ2");  
		
		admin.removeMQ();
		
		broker.close();
	}
}
