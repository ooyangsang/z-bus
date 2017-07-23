package org.zbus.examples.ha.rpc.mq;

import java.io.IOException;

import org.zbus.broker.Broker;
import org.zbus.broker.ZbusBroker;
import org.zbus.examples.rpc.appdomain.InterfaceExampleImpl;
import org.zbus.rpc.RpcProcessor;
import org.zbus.rpc.mq.Service;
import org.zbus.rpc.mq.ServiceConfig;

public class RpcService {
	@SuppressWarnings("resource")
	public static void main(String[] args) throws IOException{     
		RpcProcessor processor = new RpcProcessor();  
		processor.addModule(new InterfaceExampleImpl());  
		
		Broker broker = new ZbusBroker("127.0.0.1:16666;127.0.0.1:16667");
		//Broker broker = new ZbusBroker("127.0.0.1:15555");

		ServiceConfig config = new ServiceConfig();
		config.setConsumerCount(2); 
		config.setMq("MyRpc"); 
		config.setBroker(broker);    
		config.setMessageProcessor(processor); 
		config.setVerbose(true);
		
		Service svc = new Service(config);
		svc.start();  
	}
}
