package io.github.leoniedermeier.restclient.creation;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.springframework.core.annotation.AnnotatedElementUtils.hasAnnotation;
import static org.springframework.util.ReflectionUtils.USER_DECLARED_METHODS;
import static org.springframework.util.ReflectionUtils.getUniqueDeclaredMethods;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestOperations;

public class RestClientFactoryBean implements FactoryBean<Object>, EnvironmentAware {

    private static class MethodInvocationDispatcher implements MethodInterceptor {

        private final Map<Method, MethodMetaData> methodToMethodMetadata;

        private final RestOperations restOperations;

        public MethodInvocationDispatcher(RestOperations restOperations,
                Map<Method, MethodMetaData> methodToMethodMetadata) {
            super();
            this.restOperations = restOperations;
            this.methodToMethodMetadata = methodToMethodMetadata;
        }

        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            MethodMetaData methodMetaData = methodToMethodMetadata.get(invocation.getMethod());
            if (methodMetaData == null) {
                throw new IllegalStateException("Missing MethodMetaData for method " + invocation.getMethod());
            }
            MethodInvoker methodInvoker = new MethodInvoker(restOperations, methodMetaData);
            return methodInvoker.invoke(invocation);
        }
    }

    /**
     * Get all methods in the supplied {@link Class class} and its superclasses
     * which are annotated with the supplied {@code annotationType} but which are
     * not <em>shadowed</em> by methods overridden in subclasses.
     * <p>
     * Default methods on interfaces are also detected.
     * 
     * @param clazz          the class for which to retrieve the annotated methods
     * @param annotationType the annotation type for which to search
     * @return all annotated methods in the supplied class and its superclasses as
     *         well as annotated interface default methods
     */
    private static List<Method> getAnnotatedMethods(Class<?> clazz, Class<? extends Annotation> annotationType) {
        return Arrays.stream(getUniqueDeclaredMethods(clazz, USER_DECLARED_METHODS))
                .filter(method -> hasAnnotation(method, annotationType)).collect(toList());
    }

    private Environment environment;

    private RestOperations restOperations;

    private Class<?> type;

    private MethodInterceptor createAdvice() {
        List<Method> methods = getAnnotatedMethods(type, RequestMapping.class);

        MethodMetaDataParser parser = new MethodMetaDataParser(environment);

        Map<Method, MethodMetaData> methodToMethodMetadata = methods.stream().map(parser::parse)
                .collect(toMap(MethodMetaData::getMethod, Function.identity()));

        return new MethodInvocationDispatcher(restOperations, methodToMethodMetadata);
    }

    @Override
    public Object getObject() throws Exception {
        ProxyFactoryBean pfb = new ProxyFactoryBean();
        pfb.addInterface(type);

        pfb.addAdvice(createAdvice());
        return pfb.getObject();
    }

    @Override
    public Class<?> getObjectType() {
        return type;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public void setRestOperations(RestOperations restOperations) {
        this.restOperations = restOperations;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }
}
