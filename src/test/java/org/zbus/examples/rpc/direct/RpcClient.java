package org.zbus.examples.rpc.direct;

import org.zbus.broker.Broker;
import org.zbus.broker.ZbusBroker;
import org.zbus.examples.rpc.RpcCases;
import org.zbus.examples.rpc.appdomain.InterfaceExample;
import org.zbus.rpc.RpcFactory;

public class RpcClient {

	public static void main(String[] args) throws Exception { 
		Broker broker = new ZbusBroker("127.0.0.1:8080"); 
		
		RpcFactory factory = new RpcFactory(broker); //directly using broker as invoker 
		InterfaceExample hello = factory.getService(InterfaceExample.class);
		
		RpcCases.testDynamicProxy(hello); //test cases
		
		broker.close();
	}
}
