package io.github.leoniedermeier.restclient.creation;

import static feign.Util.checkState;
import static feign.Util.emptyToNull;
import static org.springframework.core.annotation.AnnotatedElementUtils.findMergedAnnotation;
import static org.springframework.util.StringUtils.hasText;
import static org.springframework.util.StringUtils.split;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import io.github.leoniedermeier.restclient.annotation.RestClient;
import io.github.leoniedermeier.restclient.creation.MethodMetaData.ParameterDesciption.Type;

public class MethodMetaDataParser {
    private static void checkAtMostOne(Method method, Object[] values, String fieldName) {
        checkState(values != null && (values.length == 0 || values.length == 1),
                "Method %s can only contain at most 1 %s field. Found: %s", method.getName(), fieldName,
                values == null ? null : Arrays.asList(values));
    }

    private static void checkOne(Method method, Object[] values, String fieldName) {
        checkState(values != null && values.length == 1, "Method %s can only contain 1 %s field. Found: %s",
                method.getName(), fieldName, values == null ? null : Arrays.asList(values));
    }

    private static String defaultIfEmpty(String a, String defaultString) {
        return hasText(a) ? a : defaultString;
    }

    private static void processHttpMethod(MethodMetaData methodMetaData, RequestMapping requestMapping) {
        RequestMethod[] methods = requestMapping.method();
        if (methods.length == 0) {
            methods = new RequestMethod[] { RequestMethod.GET };
        }
        checkOne(methodMetaData.getMethod(), methods, "method");
        methodMetaData.setHttpMethod(HttpMethod.resolve(methods[0].name()));
    }

    private final Environment environment;

    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public MethodMetaDataParser(Environment environment) {
        super();
        this.environment = environment;
    }

    public MethodMetaData parse(Method method) {
        MethodMetaData methodMetaData = new MethodMetaData();
        methodMetaData.setMethod(method);

        processAnnotationOnClass(methodMetaData, methodMetaData.getMethod().getDeclaringClass());

        processMethodRequestMappingAnnotation(methodMetaData);

        processMethodParametersAnnotations(methodMetaData);
        return methodMetaData;
    }

    private void processAnnotationOnClass(MethodMetaData methodMetaData, Class<?> clz) {
        if (clz.getInterfaces().length == 0) {
            RequestMapping requestMapping = findMergedAnnotation(clz, RequestMapping.class);
            if (requestMapping != null && requestMapping.value().length > 0) {
                // TODO: check At most one
                String pathValue = emptyToNull(requestMapping.value()[0]);
                pathValue = resolve(pathValue);
                methodMetaData.getPathSegments().add(0, pathValue);
            }
            RestClient restClient = findMergedAnnotation(clz, RestClient.class);
            Objects.requireNonNull(restClient, "No RestClient annotation on interface " + clz);
            Assert.hasText(restClient.url(), "No url in the RestClient annotaion on interface " + clz);
            String url = resolve(restClient.url());
            methodMetaData.setUrl(url);
        }
    }

    private void processHeaders(MethodMetaData methodMetaData, RequestMapping requestMapping) {
        for (String header : requestMapping.headers()) {
            if (header.contains("!=")) {
                continue;
            }
            String[] strings = split(header, "=");
            if (strings != null) {
                methodMetaData.getHeaders().add(resolve(strings[0]), resolve(strings[1].trim()));
            }
        }
    }

    private void processMethodParametersAnnotations(MethodMetaData methodMetaData) {
        int parameters = methodMetaData.getMethod().getParameterCount();
        for (int parameterIndex = 0; parameterIndex < parameters; parameterIndex++) {
            MethodParameter methodParameter = SynthesizingMethodParameter.forExecutable(methodMetaData.getMethod(),
                    parameterIndex);

            methodParameter.initParameterNameDiscovery(parameterNameDiscoverer);
            String parameterName = methodParameter.getParameterName();

            PathVariable pathVariable = methodParameter.getParameterAnnotation(PathVariable.class);
            if (pathVariable != null) {
                String name = defaultIfEmpty(pathVariable.value(), parameterName);
                methodMetaData.addParameterDescription(name, parameterIndex, pathVariable.required(),
                        Type.PathVariable);
            }

            RequestParam requestParam = methodParameter.getParameterAnnotation(RequestParam.class);
            if (requestParam != null) {
                String name = defaultIfEmpty(requestParam.value(), parameterName);
                methodMetaData.addParameterDescription(name, parameterIndex, requestParam.required(),
                        Type.RequestParam);
            }

            RequestHeader requestHeader = methodParameter.getParameterAnnotation(RequestHeader.class);
            if (requestHeader != null) {
                String name = defaultIfEmpty(requestHeader.value(), parameterName);
                methodMetaData.addParameterDescription(name, parameterIndex, requestHeader.required(),
                        Type.RequestHeader);
            }

            RequestBody requestBody = methodParameter.getParameterAnnotation(RequestBody.class);
            if (requestBody != null) {
                methodMetaData.addParameterDescription("__requestBody", parameterIndex, requestBody.required(),
                        Type.RequestBody);
            }
        }
        if (methodMetaData.getParameterDesciptions(Type.RequestBody).size() > 1) {
            throw new IllegalStateException(
                    "Method " + methodMetaData.getMethod() + "has more than one RequestBody annotation!");
        }
    }

    private void processMethodRequestMappingAnnotation(MethodMetaData methodMetaData) {
        RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(methodMetaData.getMethod(),
                RequestMapping.class);

        // produces / consumes are determined by the configures MessageConverters
        // org.springframework.web.client.RestTemplate.AcceptHeaderRequestCallback.doWithRequest(ClientHttpRequest)
        processHttpMethod(methodMetaData, requestMapping);
        processPath(methodMetaData, requestMapping);
        processHeaders(methodMetaData, requestMapping);
        // todo: params
    }

    private void processPath(MethodMetaData methodMetaData, RequestMapping requestMapping) {

        checkAtMostOne(methodMetaData.getMethod(), requestMapping.value(), "value");
        if (requestMapping.value().length < 1) {
            return;
        }
        String pathValue = emptyToNull(requestMapping.value()[0]);
        if (pathValue != null) {
            pathValue = resolve(pathValue);
            // Append path from @RequestMapping if value is present on method
            methodMetaData.getPathSegments().add(pathValue);
        }
    }

    private String resolve(String value) {
        if (StringUtils.hasText(value)) {
            return environment.resolvePlaceholders(value);
        }
        return value;
    }
}
