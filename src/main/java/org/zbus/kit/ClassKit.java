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
package org.zbus.kit;

import java.lang.reflect.Method;

public class ClassKit {
	public final static boolean bdbAvailable = available("com.sleepycat.je.Environment");
	public final static boolean nettyAvailable = available("io.netty.bootstrap.Bootstrap");
	
	public final static Class<?> nettyEventLoopGroupClass = load("io.netty.channel.EventLoopGroup");
	public final static Class<?> nettyNioEventLoopGroupClass = load("io.netty.channel.nio.NioEventLoopGroup");
	public final static Class<?> nettySslContextClass = load("io.netty.handler.ssl.SslContext");
	public final static Class<?> nettySslContextBuilderClass = load("io.netty.handler.ssl.SslContextBuilder");
	public final static Class<?> nettySelfSignedCertificateClass = load("io.netty.handler.ssl.util.SelfSignedCertificate");

	public static boolean available(String clazz){ 
		try {
			Class.forName(clazz); 
		} catch (ClassNotFoundException e) {
			return false;
		}
		return true;
	}
	
	public static Class<?> load(String clazz){
		try {
			return Class.forName(clazz); 
		} catch (ClassNotFoundException e) {
			//
		}
		return null;
	}
	
	public static Object invokeSimple(Object target, String name) throws Exception{
		Method m = target.getClass().getMethod(name);
		if(m == null){
			throw new IllegalArgumentException("method:("+name+") not found");
		}
		return m.invoke(target);
	}
}
