package com.ttpark.DubboServerB.service;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import org.apache.dubbo.config.annotation.Service;
import com.ttpark.DubboCommon.DubboCommonServer;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import com.ttpark.DubboServerB.redisUtil.RedisUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import com.alibaba.fastjson.JSONArray;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin
@Service
public class DubboCommonServiceImpl implements DubboCommonServer {
    @Autowired HttpClient httpClient;

    @Autowired RedisUtils redisUtils;

    @Override
    public List<Map<String, Object>> queryUser() {
        // 从redis中获取mysql查询缓存语句
        String tableName = redisUtils.get("tableName");
        // 手动埋点，拼接数据库查询指令
        Tracer tracer = GlobalTracer.get();
        Span span = tracer.buildSpan("WebServerB").start();
        String order;
        try (Scope ignored = tracer.activateSpan(span)) {
            tracer.activeSpan().setTag("methodName", "CreateMysqlOrder");
            Map<String, Object> logs = new HashMap<>(2);
            logs.put("message", "tableName=" + tableName);
            tracer.activeSpan().log(logs);
            order = "tableName=" + tableName;
        } catch (Exception e) {
            onError(e, span);
            throw e;
        } finally {
            span.finish();
        }
        try {
            HttpResponse response = httpClient.execute(new HttpGet("http://localhost:8084/queryUser?" + order));
            if (response.getStatusLine().getStatusCode() == 200) {
                // 解析数据
                HttpEntity resEntity = response.getEntity();
                List<Map<String,Object>> result = (List<Map<String,Object>>) JSONArray.parse(EntityUtils.toString(resEntity));
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void onError(Throwable throwable, Span span) {
        Tags.ERROR.set(span, Boolean.TRUE);
        if (throwable != null) {
            span.log(errorLogs(throwable));
        }
    }

    private static Map<String, Object> errorLogs(Throwable throwable) {
        Map<String, Object> errorLogs = new HashMap<>(2);
        errorLogs.put("event", Tags.ERROR.getKey());
        errorLogs.put("error.object", throwable);
        errorLogs.put("error.kind", throwable.getClass().getName());

        return errorLogs;
    }
}