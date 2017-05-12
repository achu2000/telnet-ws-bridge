package cn.easyplay.proxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class OKHttpHandler extends SimpleChannelInboundHandler<String> {
	private final static Logger LOGGER = LogManager.getLogger();
	private String url;
	private Type proxyType;
	private String proxyHost;
	private int proxyPort;
	private String userName;
	private String password;
	private okhttp3.WebSocket wsSocket;

	public OKHttpHandler(String url, Proxy.Type proxyType, String proxyHost, int proxyPort, String userName,
			String password) {
	this.url=url;
	this.proxyType=proxyType;
	this.proxyHost=proxyHost;
	this.proxyPort=proxyPort;
	this.userName=userName;
	this.password=password;
	}

	@Override
	public void channelActive(final ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
		super.channelActive(ctx);
		Builder b = new Builder();
		if (proxyType!=null && !Proxy.Type.DIRECT.equals(proxyType)) {
			b.proxy(new Proxy(proxyType,new InetSocketAddress(proxyHost,proxyPort)));
			if (StringUtils.isNotEmpty(userName)||StringUtils.isNotEmpty(password)) {
				b.proxyAuthenticator(new Authenticator() {	
					@Override
					public Request authenticate(Route arg0, Response response) throws IOException {
						 String credential = Credentials.basic(userName, password);
					       return response.request().newBuilder()
					           .header("Proxy-Authorization", credential)
					           .build();
					}
				});
			}
		}
     	OkHttpClient client=b.build();
		Request request = new Request.Builder().url(url).removeHeader("User-Agent").addHeader("User-Agent", "okhttp/3.4.1").addHeader("origin", StringUtils.replaceAll(url, "ws(s?):\\/\\/(.+)", "http$1://$2")).build();  
//		LOGGER.debug("origin="+request.header("origin"));
//        WebSocketCall webSocketCall = WebSocketCall.create(client, request);  
        client.newWebSocket(request, new WebSocketListener() {
        	@Override
        	public void onOpen(WebSocket webSocket, Response response) {
        		// TODO Auto-generated method stub
        		super.onOpen(webSocket, response);
        		LOGGER.debug("WebSocket Connected");  
                wsSocket=webSocket;
        	}
        	
        	@Override
        	public void onMessage(WebSocket webSocket, String text) {
        		// TODO Auto-generated method stub
        		super.onMessage(webSocket, text);
        		LOGGER.debug("WebSocket RECV MSG:"+text);
            	ctx.writeAndFlush(text);
        	}
		});

	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
		// TODO Auto-generated method stub
		LOGGER.debug("RECV TELNET MSG: "+msg);
//		EventLoop executor=ctx.channel().eventLoop();
		wsSocket.send(msg);
	}

}


//package cn.easyplay.proxy;
//
//import java.io.IOException;
//import java.net.InetSocketAddress;
//import java.net.Proxy;
//import java.net.Proxy.Type;
//
//import org.apache.commons.lang3.StringUtils;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//
//import io.netty.channel.ChannelHandlerContext;
//import io.netty.channel.SimpleChannelInboundHandler;
//import okhttp3.Authenticator;
//import okhttp3.Credentials;
//import okhttp3.OkHttpClient;
//import okhttp3.OkHttpClient.Builder;
//import okhttp3.Request;
//import okhttp3.RequestBody;
//import okhttp3.Response;
//import okhttp3.ResponseBody;
//import okhttp3.Route;
//import okhttp3.ws.WebSocket;
//import okhttp3.ws.WebSocketCall;
//import okhttp3.ws.WebSocketListener;
//import okio.Buffer;
//
//public class OKHttpHandler extends SimpleChannelInboundHandler<String> {
//	private final static Logger LOGGER = LogManager.getLogger();
//	private String url;
//	private Type proxyType;
//	private String proxyHost;
//	private int proxyPort;
//	private String userName;
//	private String password;
//
//	private WebSocket wsSocket;
//	
//	public OKHttpHandler(String url, Proxy.Type proxyType, String proxyHost, int proxyPort, String userName,
//			String password) {
//	this.url=url;
//	this.proxyType=proxyType;
//	this.proxyHost=proxyHost;
//	this.proxyPort=proxyPort;
//	this.userName=userName;
//	this.password=password;
//	}
//
//	@Override
//	public void channelActive(final ChannelHandlerContext ctx) throws Exception {
//		// TODO Auto-generated method stub
//		super.channelActive(ctx);
//		Builder b = new Builder();
//		if (proxyType!=null && !Proxy.Type.DIRECT.equals(proxyType)) {
//			b.proxy(new Proxy(proxyType,new InetSocketAddress(proxyHost,proxyPort)));
//			if (StringUtils.isNotEmpty(userName)||StringUtils.isNotEmpty(password)) {
//				b.proxyAuthenticator(new Authenticator() {	
//					@Override
//					public Request authenticate(Route arg0, Response response) throws IOException {
//						 String credential = Credentials.basic(userName, password);
//					       return response.request().newBuilder()
//					           .header("Proxy-Authorization", credential)
//					           .build();
//					}
//				});
//			}
//		}
//     	OkHttpClient client=b.build();
//		Request request = new Request.Builder().url(url).build();  
//        WebSocketCall webSocketCall = WebSocketCall.create(client, request);  
//        webSocketCall.enqueue(new WebSocketListener() {
//			
//
//			@Override
//			public void onPong(Buffer arg0) {
//				// TODO Auto-generated method stub
//				
//			}
//			
//			@Override
//			public void onOpen(WebSocket wsSocket, Response arg1) {
//				OKHttpHandler.this.wsSocket = wsSocket;
//				LOGGER.debug("WebSocket Connected");
//			}
//			
//			@Override
//			public void onMessage(ResponseBody response) throws IOException {
//				if (response.contentType().equals(WebSocket.TEXT)) {
//					String msg=response.string();
//					LOGGER.debug("WebSocket Recieved Text"+msg);
//					ctx.writeAndFlush(msg);
//				}
////				response.source().close();
//			}
//			
//			@Override
//			public void onFailure(IOException arg0, Response arg1) {
//				// TODO Auto-generated method stub
//				LOGGER.error(arg0);
////				ctx.close();
//			}
//			
//			@Override
//			public void onClose(int arg0, String arg1) {
//				// TODO Auto-generated method stub
//				if (ctx.channel().isOpen()) {
//					ctx.close();
//				}
//			}
//		});
//	}
//
//	@Override
//	protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
//		// TODO Auto-generated method stub
//		LOGGER.debug("RECV TELNET MSG: "+msg);
//		if ("bye".equalsIgnoreCase(msg)) {
//			//close
//			wsSocket.close(0, "bye");
//			ctx.close();
//			return;
//		}
////		EventLoop executor=ctx.channel().eventLoop();
//		RequestBody response=RequestBody.create(WebSocket.TEXT, msg);
//		wsSocket.sendMessage(response);
//	}
//	
//	@Override
//	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
//		// TODO Auto-generated method stub
////		wsSocket.close(0, "bye");		
//		super.channelInactive(ctx);
//	}
//
//}
