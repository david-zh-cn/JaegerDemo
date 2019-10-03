package com.ttpark.DubboServerB;

import io.jaegertracing.Configuration;
import io.opentracing.contrib.apache.http.client.TracingHttpClientBuilder;
import io.opentracing.util.GlobalTracer;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableDubbo(scanBasePackages = {"com.ttpark.DubboServerB.service"})
public class DubboServerBApplication {

    public static void main(String[] args) {
        SpringApplication.run(DubboServerBApplication.class, args);
    }

    @Bean
    public io.jaegertracing.internal.JaegerTracer tracer() {
        io.jaegertracing.Configuration config = new io.jaegertracing.Configuration("DubboServerB");
        io.jaegertracing.Configuration.SenderConfiguration sender = new io.jaegertracing.Configuration.SenderConfiguration();
        config.withCodec(new Configuration.CodecConfiguration().withPropagation(Configuration.Propagation.B3));
        /**
         *  从https://tracing-analysis.console.aliyun.com/ 获取jaegerd的网关（Endpoint）
         *  第一次运行时，请设置当前用户的对应的网关
         */
        sender.withEndpoint("http://localhost:14268/api/traces");
        config.withSampler(new io.jaegertracing.Configuration.SamplerConfiguration().withType("const").withParam(1));

        config.withReporter(new io.jaegertracing.Configuration.ReporterConfiguration().withSender(sender).withMaxQueueSize(10000));
        return config.getTracer();
    }

    @Bean
    public HttpClient httpClient() {
        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .setSocketTimeout(2000)
                .setConnectTimeout(3000)
                .setConnectionRequestTimeout(3000)
                .build();
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
        connManager.setMaxTotal(100);
        connManager.setDefaultMaxPerRoute(10);
        HttpClient httpClient =  TracingHttpClientBuilder.create().withTracer(GlobalTracer.get())
                .setDefaultRequestConfig(defaultRequestConfig)
                .setConnectionManager(connManager)
                .build();
        return httpClient;
    }
}
