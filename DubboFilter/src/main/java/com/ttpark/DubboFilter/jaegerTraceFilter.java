package com.ttpark.DubboFilter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Future;
import io.opentracing.Tracer;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.propagation.*;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.remoting.exchange.ResponseCallback;
import org.apache.dubbo.rpc.protocol.dubbo.FutureAdapter;
import org.apache.dubbo.rpc.support.RpcUtils;

@Activate(group = {Constants.PROVIDER, Constants.CONSUMER}, order = -1)
public class jaegerTraceFilter implements Filter {

    Tracer tracer = GlobalTracer.get();
    volatile boolean isInit = true;

    enum Kind {
        CLIENT,
        SERVER,
        PRODUCER,
        CONSUMER
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if (isInit == false) return invoker.invoke(invocation);

        RpcContext rpcContext = RpcContext.getContext();
        Kind kind = rpcContext.isProviderSide() ? Kind.SERVER : Kind.CLIENT;
        final Span span;
        if (kind.equals(Kind.CLIENT)) {
            final String operationName = "dubboClient";
            SpanContext activeSpanContext = getActiveSpanContext();
            span = createSpanFromParent(activeSpanContext, operationName);
            TextMap textMap = new TextMap() {
                @Override
                public Iterator<Map.Entry<String, String>> iterator() {
                    throw new UnsupportedOperationException(
                            "TextMapInjectAdapter should only be used with Tracer.inject()");
                }

                @Override
                public void put(String key, String value) {
                    invocation.getAttachments().put(key, value);
                }
            };
            tracer.inject(span.context(), Format.Builtin.TEXT_MAP, textMap);
        } else {
            span = getSpanFromHeaders(invocation.getAttachments(), "dubboServer");
        }

        boolean isOneway = false, deferFinish = false;
        try (Scope ignored = tracer.scopeManager().activate(span)) {
            Result result = invoker.invoke(invocation);
            if (result.hasException()) {
                onError(result.getException(), span);
            }
            isOneway = RpcUtils.isOneway(invoker.getUrl(), invocation);
            Future<Object> future = rpcContext.getFuture(); // the case on async client invocation
            if (future instanceof FutureAdapter) {
                deferFinish = true;
                ((FutureAdapter) future).getFuture().setCallback(new FinishSpanCallback(span));
            }
            return result;
        } catch (Error | RuntimeException e) {
            onError(e, span);
            throw e;
        } finally {
            if (isOneway) {
                span.finish();
            } else if (!deferFinish) {
                span.finish();
            }
        }
    }

    static void onError(Throwable throwable, Span span) {
        Tags.ERROR.set(span, Boolean.TRUE);
        if (throwable != null) {
            span.log(errorLogs(throwable));
        }
    }

    static Map<String, Object> errorLogs(Throwable throwable) {
        Map<String, Object> errorLogs = new HashMap<>(2);
        errorLogs.put("event", Tags.ERROR.getKey());
        errorLogs.put("error.object", throwable);
        errorLogs.put("error.kind", throwable.getClass().getName());

        return errorLogs;
    }

    static final class FinishSpanCallback implements ResponseCallback {
        final Span span;

        FinishSpanCallback(Span span) {
            this.span = span;
        }

        @Override public void done(Object response) {
            span.finish();
        }

        @Override public void caught(Throwable exception) {
            onError(exception, span);
            span.finish();
        }
    }

    private SpanContext getActiveSpanContext() {
        if (tracer.activeSpan() != null) {
            return tracer.activeSpan().context();
        }
        return null;
    }

    private Span createSpanFromParent(SpanContext parentSpanContext, String operationName) {
        final Tracer.SpanBuilder spanBuilder;
        if (parentSpanContext == null) {
            spanBuilder = tracer.buildSpan(operationName);
        } else {
            spanBuilder = tracer.buildSpan(operationName).asChildOf(parentSpanContext);
        }
        return spanBuilder
                .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT)
                .start();
    }

    Span getSpanFromHeaders(Map<String, String> attachments, String operationName) {
        Map<String, Object> fields = null;
        Tracer.SpanBuilder spanBuilder = tracer.buildSpan(operationName);
        try {
            SpanContext parentSpanCtx = tracer.extract(Format.Builtin.TEXT_MAP, new TextMapAdapter(attachments));
            if (parentSpanCtx != null) {
                spanBuilder = spanBuilder.asChildOf(parentSpanCtx);
            }
        } catch (IllegalArgumentException iae) {
            spanBuilder = spanBuilder.withTag(Tags.ERROR, Boolean.TRUE);
        }
        Span span = spanBuilder
                .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER)
                .withTag(Tags.COMPONENT.getKey(), "java-dubbo")
                .start();
        if (fields != null) {
            span.log(fields);
        }
        return span;
    }

}
