package io.reconark.kernel.runtime;

import io.reconark.kernel.api.ExtensionPoint;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/** Wraps extension implementations in a JDK proxy that runs the interceptor chain. */
final class Interception {

    private Interception() {}

    static <T> T wrap(ExtensionPoint<T> point, String key, String pluginId, T target, List<ExtensionInterceptor> chain) {
        if (chain.isEmpty()) {
            return target;
        }
        InvocationHandler handler = (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return invokeObjectMethod(proxy, target, method, args);
            }
            ExtensionInterceptor.Call call = new ExtensionInterceptor.Call(point.id(), key, pluginId, method.getName());
            return proceed(chain, 0, call, () -> invoke(target, method, args));
        };
        Object proxy = Proxy.newProxyInstance(point.type().getClassLoader(), new Class<?>[] {point.type()}, handler);
        return point.type().cast(proxy);
    }

    private static Object proceed(
            List<ExtensionInterceptor> chain, int i, ExtensionInterceptor.Call call, ExtensionInterceptor.Invocation last)
            throws Throwable {
        if (i == chain.size()) {
            return last.proceed();
        }
        return chain.get(i).around(call, () -> proceed(chain, i + 1, call, last));
    }

    private static Object invoke(Object target, Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    private static Object invokeObjectMethod(Object proxy, Object target, Method method, Object[] args) throws Throwable {
        return switch (method.getName()) {
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> "Intercepted[" + target + "]";
            default -> invoke(target, method, args);
        };
    }
}
