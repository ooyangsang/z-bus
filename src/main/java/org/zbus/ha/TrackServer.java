/**
 * The MIT License (MIT)
 * Copyright (c) 2009-2015 HONG LEIMING
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.zbus.ha;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.zbus.kit.ConfigKit;
import org.zbus.kit.log.Logger;
import org.zbus.net.Client.ConnectedHandler;
import org.zbus.net.Client.DisconnectedHandler;
import org.zbus.net.EventDriver;
import org.zbus.net.Session;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageHandler;
import org.zbus.net.http.MessageAdaptor;
import org.zbus.net.http.MessageClient;
import org.zbus.net.http.MessageServer;

public class TrackServer extends MessageAdaptor implements Closeable{
	private static final Logger log = Logger.getLogger(TrackServer.class); 
	
	private final ServerEntryTable serverEntryTable = new ServerEntryTable(); 
	private ScheduledExecutorService dumpExecutor = Executors.newSingleThreadScheduledExecutor();
	private boolean verbose = false;
	private int dumpInterval = 3000;
	
	private Map<String, Session> trackSubs =  new ConcurrentHashMap<String, Session>();
	private Map<String, MessageClient> joinedServers = new ConcurrentHashMap<String, MessageClient>();
	
	private EventDriver eventDriver;
	private MessageServer server;
	private String serverHost = "0.0.0.0";
	private int serverPort = 16666;
	
	public TrackServer(int port){
		this("0.0.0.0", port, null);
	}
	public TrackServer(String host, int port){
		this(host, port, null);
	}
	
	public TrackServer(String host, int port, EventDriver driver){  
		this.serverHost = host;
		this.serverPort = port;
		this.eventDriver = driver;
		
		cmd(HaCommand.EntryUpdate, entryUpdateHandler); 
		cmd(HaCommand.EntryRemove, entryRemoveHandler); 

		cmd(HaCommand.ServerJoin, serverJoinHandler);
		cmd(HaCommand.ServerLeave, serverLeaveHandler);

		cmd(HaCommand.SubAll, subAllHandler);
	}   
	
	public void start() throws Exception{
		server = new MessageServer(eventDriver);
		server.start(serverHost, serverPort, this);
		eventDriver = server.getEventDriver(); //if eventDriver is null, set the internal created one
	
		dumpExecutor.scheduleAtFixedRate(new Runnable() { 
			@Override
			public void run() { 
				if(verbose) serverEntryTable.dump();
			}
		}, 1000, dumpInterval, TimeUnit.MILLISECONDS);
	}
	
	public void setDumpInterval(int dumpInterval) {
		this.dumpInterval = dumpInterval;
	}
	
	private void pubMessage(Message msg){
		Iterator<Entry<String, Session>> iter = trackSubs.entrySet().iterator();
		while(iter.hasNext()){
			Entry<String, Session> entry = iter.next();
			Session sub = entry.getValue();
			try{
				msg.removeHead(Message.ID);
				sub.write(msg);
			}catch(Exception e){
				iter.remove();
				log.error(e.getMessage(), e);
			}
		}
	}
	
	private MessageHandler serverJoinHandler = new MessageHandler() { 
		@Override
		public void handle(Message msg, Session sess) throws IOException { 
			if(verbose){
				log.info("%s", msg);
			}
			String serverAddr = msg.getServer();
			if(serverAddr == null) return;
			onNewServer(serverAddr, sess); 
		}
	};
	
	private MessageHandler serverLeaveHandler = new MessageHandler() { 
		@Override
		public void handle(Message msg, Session sess) throws IOException { 
			if(verbose){
				log.info("%s", msg);
			}
			
			String serverAddr = msg.getServer();
			if(serverAddr == null) return; 
			serverEntryTable.removeServer(serverAddr);
			
			pubMessage(msg);
		}
	};
	
	private MessageHandler entryUpdateHandler = new MessageHandler() { 
		@Override
		public void handle(Message msg, Session sess) throws IOException { 
			if(verbose){
				log.info("%s", msg);
			}
			ServerEntry se = null;
			try{
				se = ServerEntry.unpack(msg.getBodyString());
			} catch(Exception e){
				log.error(e.getMessage(), e);
				return;
			}
			boolean isNewServer = serverEntryTable.isNewServer(se);
			
			serverEntryTable.updateServerEntry(se);
			if(isNewServer){
				onNewServer(se.serverAddr, sess);
			}
			
			pubMessage(msg);
		}
	}; 
	
	private MessageHandler entryRemoveHandler = new MessageHandler() { 
		@Override
		public void handle(Message msg, Session sess) throws IOException {
			String serverAddr = msg.getServer();
			String entryId = msg.getMq(); //TODO use mq for entryId
			serverEntryTable.removeServerEntry(serverAddr, entryId);
			
			pubMessage(msg);
		}
	}; 
	
	private MessageHandler subAllHandler = new MessageHandler() {
		@Override
		public void handle(Message msg, Session sess) throws IOException {
			if(verbose){
				log.info("%s", msg);
			}
			trackSubs.put(sess.id(), sess);   
			
			Message m = new Message(); 
			m.setStatus(200);
			m.setCmd(HaCommand.PubAll);
			m.setBody(serverEntryTable.pack());
			sess.write(m);
		}
	};
	
	private void onNewServer(final String serverAddr, Session sess) throws IOException{
		
		synchronized (joinedServers) {
			if(joinedServers.containsKey(serverAddr)) return; 
			 
			log.info(">>New Server: "+ serverAddr); 
			
			final MessageClient client = new MessageClient(serverAddr, eventDriver);
			joinedServers.put(serverAddr, client); 
			
			client.onConnected(new ConnectedHandler() { 
				@Override
				public void onConnected() throws IOException { 
					log.info("Connection to (%s) OK", serverAddr);
					joinedServers.put(serverAddr, client); 
					
					serverEntryTable.addServer(serverAddr);
					Message msg = new Message();
					msg.setCmd(HaCommand.ServerJoin);
					msg.setServer(serverAddr);
					pubMessage(msg);
				}
			});
			
			client.onDisconnected(new DisconnectedHandler() { 
				@Override
				public void onDisconnected() throws IOException {
					log.warn("Server(%s) down", serverAddr);
					joinedServers.remove(serverAddr); 
					serverEntryTable.removeServer(serverAddr);   
					
					client.close();
					log.info("Sending ServerLeave message");
					Message msg = new Message();
					msg.setCmd(HaCommand.ServerLeave);
					msg.setServer(serverAddr);
					
					pubMessage(msg);
				} 
			}); 
			
			client.ensureConnectedAsync();
		}
	}
	
	public void onSessionToDestroy(Session sess) throws IOException {
		log.info("Remove " + sess);
		trackSubs.remove(sess.id()); 
	}
	
	@Override
	public void onException(Throwable e, Session sess) throws Exception {
		trackSubs.remove(sess.id());
		super.onException(e, sess);
	}
	
	@Override
	public void close() throws IOException { 
		if(dumpExecutor != null){
			dumpExecutor.shutdown(); 
			dumpExecutor = null;
		}
		if(server != null){
			server.close();
			server = null;
		}
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose; 
	} 
	
	
	public static void main(String[] args) throws Exception { 
		String host = ConfigKit.option(args, "-h", "0.0.0.0");
		int port = ConfigKit.option(args, "-p", 16666);
		boolean verbose = ConfigKit.option(args, "-verbose", true);
		
		final TrackServer server = new TrackServer(host, port);
		server.setVerbose(verbose); 
		server.start();
		
		Runtime.getRuntime().addShutdownHook(new Thread(){ 
			public void run() { 
				try {
					server.close();
					log.info("TrackServer shutdown completed");
				} catch (IOException e) {
					log.error(e.getMessage(), e);
				}
			}
		});   
	}  
}
