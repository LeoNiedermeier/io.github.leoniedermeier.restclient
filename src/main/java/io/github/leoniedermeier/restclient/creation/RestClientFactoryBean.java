package io.github.leoniedermeier.restclient.creation;

import static io.github.leoniedermeier.restclient.creation.MethodMetaData.ParameterDesciption.Type.PathVariable;
import static io.github.leoniedermeier.restclient.creation.MethodMetaData.ParameterDesciption.Type.RequestParam;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.springframework.core.annotation.AnnotatedElementUtils.hasAnnotation;
import static org.springframework.util.ReflectionUtils.USER_DECLARED_METHODS;
import static org.springframework.util.ReflectionUtils.getUniqueDeclaredMethods;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestOperations;
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

		private final MethodMetaData methodMetaData;
		private final RestOperations restTemplate;

		public XyzMethodInterceptor(MethodMetaData methodMetaData, RestOperations restTemplate) {
			super();
			this.methodMetaData = methodMetaData;
			this.restTemplate = restTemplate;
		}

		@Override
		public Object invoke(MethodInvocation invocation) throws Throwable {
			URI uriString = buildUriString(invocation);
			HttpEntity<?> requestEntity = new HttpEntity<>(methodMetaData.getHeaders());
			ParameterizedTypeReference<?> responseType = ParameterizedTypeReference
					.forType(methodMetaData.getMethod().getGenericReturnType());

			ResponseEntity<?> result = restTemplate.exchange(uriString, methodMetaData.getHttpMethod(), requestEntity,
					responseType);
			return result.getBody();
		}

		private URI buildUriString(MethodInvocation invocation) {
			UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(methodMetaData.getUrl());

			methodMetaData.getPathSegments().forEach(uriComponentsBuilder::path);

			methodMetaData.getParameterDesciptions(RequestParam).forEach(
					pd -> uriComponentsBuilder.queryParam(pd.getName(), invocation.getArguments()[pd.getIndex()]));

			Map<String, Object> uriVariables = methodMetaData.getParameterDesciptions(PathVariable).stream()
					.collect(toMap(pd -> pd.getName(), pd -> invocation.getArguments()[pd.getIndex()]));

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
		return Arrays.stream(getUniqueDeclaredMethods(clazz, USER_DECLARED_METHODS))
				.filter(method -> hasAnnotation(method, annotationType)).collect(toList());
	}

	private Environment environment;

	private RestOperations restOperations;

	private Class<?> type;

	@Override
	public Object getObject() throws Exception {
		ProxyFactoryBean pfb = new ProxyFactoryBean();
		pfb.addInterface(type);
		List<Method> methods = getAnnotatedMethods(type, RequestMapping.class);

		MethodMetaDataParser parser = new MethodMetaDataParser(environment);

		Map<Method, MethodInterceptor> methodToMethodInterceptor = methods.stream().map(parser::parse)
				.collect(toMap(MethodMetaData::getMethod, md -> new XyzMethodInterceptor(md, restOperations)));

		pfb.addAdvice(new MethodInvocationDispatcher(methodToMethodInterceptor));
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
