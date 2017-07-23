package org.zbus.net.tcp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.zbus.net.IoAdaptor;
import org.zbus.net.Session;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

@Sharable
public class NettyToIoAdaptor extends ChannelInboundHandlerAdapter {
	private final static AttributeKey<String> sessionKey = AttributeKey.valueOf("session");
	private Map<String, Session> sessionMap = new ConcurrentHashMap<String, Session>();

	private final IoAdaptor ioAdaptor; 
	public NettyToIoAdaptor(IoAdaptor ioAdaptor){
		this.ioAdaptor = ioAdaptor;
	}
	
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		Session sess = getSession(ctx);
		ioAdaptor.onMessage(msg, sess);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		Session sess = getSession(ctx);
		ioAdaptor.onException(cause, sess);
	}
	 
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		Session sess = attachSession(ctx);
		ioAdaptor.onSessionAccepted(sess);
		ioAdaptor.onSessionConnected(sess);
	}  
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		Session sess = getSession(ctx);
		ioAdaptor.onSessionToDestroy(sess);
	} 
	
	private Session attachSession(ChannelHandlerContext ctx){
		Session sess = new TcpSession(ctx); 
		Attribute<String> attr = ctx.attr(sessionKey); 
		attr.set(sess.id()); 
		sessionMap.put(sess.id(), sess);
		return sess;
	}
	
	private Session getSession(ChannelHandlerContext ctx){
		Attribute<String> attr = ctx.attr(sessionKey); 
		if(attr.get() == null){
			throw new IllegalThreadStateException("Missing sessionKey");
		}
		Session sess = sessionMap.get(attr.get()); 
		if(sess == null){
			throw new IllegalThreadStateException("Session and ChannelHandlerContext mapping not found");
		}
		return sess;
	}
}
