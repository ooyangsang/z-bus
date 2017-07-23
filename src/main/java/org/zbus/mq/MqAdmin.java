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
package org.zbus.mq;

import java.io.IOException;

import org.zbus.broker.Broker;
import org.zbus.mq.Protocol.MqMode;
import org.zbus.net.http.Message;


public class MqAdmin{     
	protected final Broker broker;      
	protected String mq;   
	protected final int mode;  
	protected String accessToken = "";
	protected String registerToken = "";
	protected String masterMq = null;
	protected String masterToken = null;
	
	public MqAdmin(Broker broker, String mq, MqMode... mode){  
		this.broker = broker;
		this.mq = mq;  
		if(mode.length == 0){
			this.mode = MqMode.intValue(MqMode.MQ); 
		} else {
			this.mode = MqMode.intValue(mode);
		} 
	} 
	
	public MqAdmin(MqConfig config){
		this.broker = config.getBroker();
		this.mq = config.getMq(); 
		this.mode = config.getMode();
		this.accessToken = config.getAccessToken();
		this.registerToken = config.getRegisterToken();
		this.masterMq = config.getMasterMq();
		this.masterToken = config.getMasterToken();
	} 
	 
	protected Message invokeSync(Message req) throws IOException, InterruptedException{
		return broker.invokeSync(req, 10000);
	}
	
	public Message queryMQ() throws IOException, InterruptedException{
		Message req = new Message();
    	req.setCmd(Protocol.QueryMQ); 
    	req.setMq(this.mq); 
    	req.setHead("register_token", registerToken); 
    	req.setHead("access_token", accessToken);
    	
    	return invokeSync(req); 
	}
    
    public boolean createMQ() throws IOException, InterruptedException{
    	Message req = new Message();
    	req.setCmd(Protocol.CreateMQ); 
    	req.setHead("mq_name", mq);
    	req.setHead("mq_mode", "" + mode);  
    	req.setHead("register_token", registerToken); 
    	req.setHead("access_token", accessToken);
    	req.setMasterMq(masterMq);
    	req.setMasterToken(masterToken);
    	
    	Message res = invokeSync(req);
    	if(res == null) return false;
    	return res.isStatus200();
    } 
    
    public boolean removeMQ() throws IOException, InterruptedException{
    	Message req = new Message();
    	req.setCmd(Protocol.RemoveMQ); 
    	req.setHead("mq_name", mq); 
    	req.setHead("register_token", registerToken);  
    	
    	Message res = invokeSync(req);
    	if(res == null) return false;
    	return res.isStatus200();
    } 
    
    public int addKey(String key) throws IOException, InterruptedException{
    	return addKey(null, key);
    }
    
    public int addKey(String keyGroup, String key) throws IOException, InterruptedException{
    	return addKey(this.mq, keyGroup, key);
    }
    
    public int addKey(String mq, String keyGroup, String key) throws IOException, InterruptedException{
    	Message req = new Message();
    	req.setCmd(Protocol.AddKey); 
    	req.setMq(mq);
    	req.setKeyGroup(keyGroup);
    	req.setKey(key);
    	req.setHead("register_token", registerToken); 
    	
    	Message res = invokeSync(req);
    	if(res == null) return 0;
    	if(!res.isStatus200()){
    		return 0;
    	}
    	return Integer.valueOf(res.getBodyString());
    }
    
    public int removeKey(String key) throws IOException, InterruptedException{
    	return removeKey(this.mq, null, key);
    }
    
    public int removeKey(String keyGroup, String key) throws IOException, InterruptedException{
    	return removeKey(this.mq, keyGroup, key);
    }
    
    public int removeKey(String mq, String keyGroup, String key) throws IOException, InterruptedException{
    	Message req = new Message();
    	req.setCmd(Protocol.RemoveKey);
    	req.setMq(mq);
    	req.setKeyGroup(keyGroup);
    	req.setKey(key);
    	req.setHead("register_token", registerToken); 
    	
    	Message res = invokeSync(req);
    	if(res == null) return 0;
    	if(!res.isStatus200()){
    		return 0;
    	}
    	return Integer.valueOf(res.getBodyString());
    } 
    
    
    public int removeGroup(String mq, String keyGroup) throws IOException, InterruptedException{
    	return removeKey(mq, keyGroup, null);
    }
    
    public int removeGroup(String keyGroup) throws IOException, InterruptedException{
    	return removeKey(this.mq, keyGroup, null);
    }
    
	public String getMq() {
		return mq;
	}

	public void setMq(String mq) {
		this.mq = mq;
	}

	public int getMode() {
		return mode;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getRegisterToken() {
		return registerToken;
	}

	public void setRegisterToken(String registerToken) {
		this.registerToken = registerToken;
	}

	public String getMasterMq() {
		return masterMq;
	}

	public void setMasterMq(String masterMq) {
		this.masterMq = masterMq;
	}

	public String getMasterToken() {
		return masterToken;
	}

	public void setMasterToken(String masterToken) {
		this.masterToken = masterToken;
	} 
	
}
