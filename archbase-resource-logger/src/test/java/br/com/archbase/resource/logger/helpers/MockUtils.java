package br.com.archbase.resource.logger.helpers;

import br.com.archbase.resource.logger.bean.RequestContext;
import br.com.archbase.resource.logger.bean.User;
import br.com.archbase.resource.logger.utils.RequestUtil;
import com.google.common.collect.ImmutableMap;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

public class MockUtils {

    public static List<Object> mockWorkflow(@Nonnull ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        MethodSignature methodSignature = mockMethodSignature();
        mockProceedingJoinPoint(proceedingJoinPoint, methodSignature);

        List<Object> mockedObjects = new ArrayList<>();
        mockedObjects.add(methodSignature);
        mockedObjects.add(proceedingJoinPoint);
        return mockedObjects;
    }

    public static void mockProceedingJoinPoint(
            @Nonnull ProceedingJoinPoint joinPoint,
            @Nonnull MethodSignature signature
    ) throws Throwable {
        mockProceedingJoinPoint(
                joinPoint,
                new User(1, "joao@example.com.br", "password"),
                signature,
                new DummyResource(),
                new Object[]{1});
    }

    public static void mockProceedingJoinPoint(
            @Nonnull ProceedingJoinPoint joinPoint,
            @Nullable Object returnValue,
            @Nonnull MethodSignature signature,
            @Nonnull Object target,
            @Nonnull Object[] methodParamValues
    ) throws Throwable {
        when(joinPoint.proceed()).thenReturn(returnValue);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getTarget()).thenReturn(target);
        when(joinPoint.getArgs()).thenReturn(methodParamValues);
    }

    public static MethodSignature mockMethodSignature() throws NoSuchMethodException {
        return mockMethodSignature(
                "getUser",
                User.class,
                new String[]{"userId"},
                new Class[]{int.class},
                DummyResource.class
        );
    }

    public static MethodSignature mockMethodSignature(
            @Nonnull String methodName,
            @Nonnull Class returnType,
            @Nonnull String[] parameterNames,
            @Nonnull Class[] parameterTypes,
            @Nonnull Class target
    ) throws NoSuchMethodException {
        MethodSignature methodSignature = mock(MethodSignature.class, RETURNS_DEEP_STUBS);
        when(methodSignature.getName()).thenReturn(methodName);
        when(methodSignature.getReturnType()).thenReturn(returnType);
        when(methodSignature.getParameterNames()).thenReturn(parameterNames);

        Method method = target.getMethod(methodName, parameterTypes);
        when(methodSignature.getMethod()).thenReturn(method);

        return methodSignature;
    }

    @Nonnull
    public static RequestUtil mockRequestUtil() {
        return mockRequestUtil(ImmutableMap.of(
                "url", "https://www.example.com.br",
                "username", "João da Silva")
        );
    }

    @Nonnull
    public static RequestUtil mockRequestUtil(@Nonnull Map<String, String> context) {
        RequestUtil requestUtil = mock(RequestUtil.class);
        doReturn(new RequestContext(context)).when(requestUtil).getRequestContext();

        return requestUtil;
    }
}
