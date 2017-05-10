package cn.easyplay.proxy;

import java.nio.charset.Charset;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

public class TelentProxyServer {
	
    private static final StringDecoder DECODER = new StringDecoder(Charset.forName("GBK"));
    private static final StringEncoder ENCODER = new StringEncoder(Charset.forName("GBK"));
    
	private boolean SSL = System.getProperty("ssl") != null;
	private int PORT = Integer.parseInt(System.getProperty("port", SSL ? "8992" : "8180"));
	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;
	private ServerBootstrap b;

	public TelentProxyServer() {

	}

	public void startup() throws Exception {
		// Configure SSL.
		final SslContext sslCtx;
		if (SSL) {
			SelfSignedCertificate ssc = new SelfSignedCertificate();
			sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
		} else {
			sslCtx = null;
		}

		bossGroup = new NioEventLoopGroup(1);
		workerGroup = new NioEventLoopGroup();
		b = new ServerBootstrap();
		b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).handler(new LoggingHandler(LogLevel.INFO))
				.childHandler(new ChannelInitializer<Channel>() {
					protected void initChannel(Channel ch) throws Exception {
						ChannelPipeline pipeline = ch.pipeline();

				        if (sslCtx != null) {
				            pipeline.addLast(sslCtx.newHandler(ch.alloc()));
				        }

				        // Add the text line codec combination first,
//				        pipeline.addLast(new LineBasedFrameDecoder(8192));
				        pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
				        // the encoder and decoder are static as these are sharable
				        pipeline.addLast(DECODER);
				        pipeline.addLast(ENCODER);

//				        TelnetProxyFrontHandler PROXY_HANDLER = new TelnetProxyFrontHandler("ws://www.palmmud.com:8080");
//				        // and then business logic.
//				        pipeline.addLast(PROXY_HANDLER);
				        OKHttpHandler proxyHandler=new OKHttpHandler("ws://www.palmmud.com:8080", null, null, -1, null, null);
				        pipeline.addLast(proxyHandler);
					};
				});

		b.bind(PORT).sync();
	}

	public void shutdown() {
		bossGroup.shutdownGracefully();
		workerGroup.shutdownGracefully();
	}
	
	public static void main(String[] args) throws Exception {
		new TelentProxyServer().startup();
	}

}