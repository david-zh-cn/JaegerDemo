package com.ttpark.WebServerC;

import io.jaegertracing.Configuration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class WebServerCApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebServerCApplication.class, args);
    }
    @Bean
    public io.jaegertracing.internal.JaegerTracer tracer() {
        io.jaegertracing.Configuration config = new io.jaegertracing.Configuration("WebServerC");
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
}