package cn.easyplay.proxy;

import java.nio.charset.Charset;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslContext;

public class TelnetProxyInitializer extends ChannelInitializer<SocketChannel> {

    private static final StringDecoder DECODER = new StringDecoder(Charset.forName("GBK"));
    private static final StringEncoder ENCODER = new StringEncoder(Charset.forName("GBK"));

    private final SslContext sslCtx;

    public TelnetProxyInitializer(SslContext sslCtx) {
        this.sslCtx = sslCtx;
    }

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        if (sslCtx != null) {
            pipeline.addLast(sslCtx.newHandler(ch.alloc()));
        }

        // Add the text line codec combination first,
//        pipeline.addLast(new LineBasedFrameDecoder(8192));
        pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
        // the encoder and decoder are static as these are sharable
        pipeline.addLast(DECODER);
        pipeline.addLast(ENCODER);

        TelnetProxyFrontHandler PROXY_HANDLER = new TelnetProxyFrontHandler("ws://www.palmmud.com:8080");
        // and then business logic.
        pipeline.addLast(PROXY_HANDLER);
    }
}
