package io.github.leoniedermeier.restclient.annotation;

import static io.github.leoniedermeier.restclient.annotation.MethodMetaData.ParameterDesciption.Type.PathVariable;
import static io.github.leoniedermeier.restclient.annotation.MethodMetaData.ParameterDesciption.Type.RequestParam;
import static java.util.stream.Collectors.toMap;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public class RestClientFactoryBean implements FactoryBean<Object>, EnvironmentAware {

    static class MethodInvocationDispatcher implements MethodInterceptor {
        private Map<Method, MethodInterceptor> methodToMethodInterceptor;

        public MethodInvocationDispatcher(Map<Method, MethodInterceptor> methodToMethodInterceptor) {
            super();
            this.methodToMethodInterceptor = methodToMethodInterceptor;
        }

        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            MethodInterceptor interceptor = methodToMethodInterceptor.get(invocation.getMethod());
            if (interceptor == null) {
                throw new IllegalStateException("");
            }
            return interceptor.invoke(invocation);
        }

    }

    static class XyzMethodInterceptor implements MethodInterceptor {

        private MethodMetaData methodMetaData;
        private RestTemplate restTemplate;

        public XyzMethodInterceptor(MethodMetaData methodMetaData, RestTemplate restTemplate) {
            super();
            this.methodMetaData = methodMetaData;
            this.restTemplate = restTemplate;
        }

        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            // alles aus MethodMetaData

            URI uriString = buildUriString(invocation);

            System.out.println(">>>>>>>>>>");
            System.out.println(uriString);
            System.out.println(">>>>>>>>>>");
            HttpEntity<?> requestEntity = new HttpEntity<>(methodMetaData.getHeaders());
            ParameterizedTypeReference<?> responseType = ParameterizedTypeReference
                    .forType(methodMetaData.getMethod().getGenericReturnType());
            ResponseEntity<?> exchange = restTemplate.exchange(uriString, methodMetaData.getHttpMethod(), requestEntity,
                    responseType);

            System.out.println(exchange);

            return exchange.getBody();
        }

        private URI buildUriString(MethodInvocation invocation) {
            UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(methodMetaData.getUrl());

            methodMetaData.getPathSegments().forEach(uriComponentsBuilder::path);

            methodMetaData.getParameterDesciptions(RequestParam)
                    .forEach(pd -> uriComponentsBuilder.queryParam(pd.name, invocation.getArguments()[pd.index]));

            Map<String, Object> uriVariables = methodMetaData.getParameterDesciptions(PathVariable)
                    .collect(toMap(pd -> pd.name, pd -> invocation.getArguments()[pd.index]));

            return uriComponentsBuilder.buildAndExpand(uriVariables).toUri();

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
        return Arrays.stream(ReflectionUtils.getUniqueDeclaredMethods(clazz, ReflectionUtils.USER_DECLARED_METHODS))
                .filter(method -> AnnotatedElementUtils.hasAnnotation(method, annotationType))
                .collect(Collectors.toList());
    }

    private Environment environment;

    private RestTemplate restTemplate;
    
    private Class<?> type;
    @Override
    public Object getObject() throws Exception {
        ProxyFactoryBean pfb = new ProxyFactoryBean();
        pfb.addInterface(type);
        List<Method> methods = getAnnotatedMethods(type, RequestMapping.class);

        MethodMetaDataParser parser = new MethodMetaDataParser(environment);
     

        Map<Method, MethodInterceptor> map =  methods.stream().map(parser::parse)
                .collect(toMap(MethodMetaData::getMethod, md -> new XyzMethodInterceptor(md, restTemplate)));

        pfb.addAdvice(new MethodInvocationDispatcher(map));
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

    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }
}
