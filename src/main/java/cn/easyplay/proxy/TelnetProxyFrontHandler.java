package cn.easyplay.proxy;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidParameterException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

public class TelnetProxyFrontHandler extends SimpleChannelInboundHandler<String> {

	private final static Logger LOGGER = LogManager.getLogger();
	private URI uri = null;
	private boolean ssl;
	private String host;
	private int port;
	private SslContext sslCtx = null;

	private Channel websocketChannel;

	public TelnetProxyFrontHandler(String remoteWsUrl) throws URISyntaxException {
		this.uri = new URI(remoteWsUrl);
		String scheme = uri.getScheme() == null ? "ws" : uri.getScheme();
		this.host = uri.getHost() == null ? "127.0.0.1" : uri.getHost();
		this.port = this.uri.getPort();
		if (this.port == -1) {
			if ("ws".equalsIgnoreCase(scheme)) {
				this.port = 80;
			} else if ("wss".equalsIgnoreCase(scheme)) {
				this.port = 443;
			}
		}

		if (!"ws".equalsIgnoreCase(scheme) && !"wss".equalsIgnoreCase(scheme)) {
			System.err.println("Only WS(S) is supported.");
			throw new InvalidParameterException("Only WS(S) is supported.");
		}

		this.ssl = "wss".equalsIgnoreCase(scheme);
	}

	@Override
	public void channelActive(final ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
		super.channelActive(ctx);
		if (ssl) {
			sslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
		}

		// 启动WebSocketClient
		EventLoopGroup group = ctx.channel().eventLoop();
		final WebSocketClientHandler handler = new WebSocketClientHandler(WebSocketClientHandshakerFactory
				.newHandshaker(uri, WebSocketVersion.V13, null, true, new DefaultHttpHeaders()), ctx.channel());
		LOGGER.debug("WebSocketClient启动");
		Bootstrap b = new Bootstrap();
		b.group(group).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
			@Override
			protected void initChannel(SocketChannel ch) {
				ChannelPipeline p = ch.pipeline();
				websocketChannel = ch;
				if (sslCtx != null) {
					p.addLast(sslCtx.newHandler(ch.alloc(), host, port));
				}
				p.addLast(new HttpClientCodec(), new HttpObjectAggregator(8192),
						WebSocketClientCompressionHandler.INSTANCE, handler);
			}
		});
		b.connect(uri.getHost(), port).addListener(new ChannelFutureListener() {
			
			@Override
			public void operationComplete(ChannelFuture arg0) throws Exception {
				// TODO Auto-generated method stub
				if (arg0.isSuccess()) {
					LOGGER.debug("WebSocketClient Socket已连接");
					handler.handshakeFuture().addListener(new ChannelFutureListener() {
						
						@Override
						public void operationComplete(ChannelFuture arg0) throws Exception {
							// TODO Auto-generated method stub
							if (arg0.isSuccess()) {
								LOGGER.debug("WebSocketClient握手成功！");
							}
						}
					});
				}
			}
		});
		
	}


	@Override
	protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
		LOGGER.debug("Telnet Recieved MSG: "+msg);
		if ("bye".equalsIgnoreCase(msg)) {
			ctx.close();
			websocketChannel.close();
			return;
		}
		websocketChannel.writeAndFlush(new TextWebSocketFrame(msg));
		
	}

}
