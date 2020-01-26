package io.github.leoniedermeier.restclient.annotation;

import static feign.Util.checkState;
import static feign.Util.emptyToNull;
import static org.springframework.core.annotation.AnnotatedElementUtils.findMergedAnnotation;
import static org.springframework.util.StringUtils.hasText;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import io.github.leoniedermeier.restclient.annotation.MethodMetaData.ParameterDesciption.Type;

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

    private static String firstElementOrNull(String[] elements) {
        if (elements == null || elements.length == 0 || elements[0] == null || elements[0].isEmpty()) {
            return null;
        }
        return elements[0];

    }

    private static void processConsumes(MethodMetaData methodMetaData, RequestMapping requestMapping) {
        String[] serverConsumes = requestMapping.consumes();
        String clientProduces = firstElementOrNull(serverConsumes);
        if (clientProduces != null) {
            methodMetaData.getHeaders().add(HttpHeaders.CONTENT_TYPE, clientProduces);
        }
    }

    private static void processHttpMethod(MethodMetaData methodMetaData, RequestMapping requestMapping) {
        RequestMethod[] methods = requestMapping.method();
        if (methods.length == 0) {
            methods = new RequestMethod[] { RequestMethod.GET };
        }
        checkOne(methodMetaData.getMethod(), methods, "method");
        methodMetaData.setHttpMethod(HttpMethod.resolve(methods[0].name()));
    }

    private static void processProduces(MethodMetaData methodMetaData, RequestMapping requestMapping) {
        String[] serverProduces = requestMapping.produces();
        String clientAccepts = firstElementOrNull(serverProduces);
        if (clientAccepts != null) {
            methodMetaData.getHeaders().add(HttpHeaders.ACCEPT, clientAccepts);
        }
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
        // path
        processAnnotationOnClass(methodMetaData, methodMetaData.getMethod().getDeclaringClass());

        processMethodRequestMappingAnnotation(methodMetaData);

        processMethodParametersAnnotations(methodMetaData);
        return methodMetaData;
    }

    private void processAnnotationOnClass(MethodMetaData methodMetaData, Class<?> clz) {
        if (clz.getInterfaces().length == 0) {
            RequestMapping requestMapping = findMergedAnnotation(clz, RequestMapping.class);
            if (requestMapping != null && requestMapping.value().length > 0) {
                String pathValue = emptyToNull(requestMapping.value()[0]);
                pathValue = resolve(pathValue);
                methodMetaData.getPathSegments().add(0, pathValue);
            }
            RestClient restClient = findMergedAnnotation(clz, RestClient.class);
            Objects.requireNonNull(restClient, "Meldubng");
            Objects.requireNonNull(restClient.url(), "Meldubng");
            String url = resolve(restClient.url());
            methodMetaData.setUrl(url);
        }
    }

    private void processHeaders(MethodMetaData methodMetaData, RequestMapping requestMapping) {
        for (String header : requestMapping.headers()) {
            if (header.contains("!=")) {
                continue;
            }
            String[] strings = StringUtils.split(header, "=");
            if (strings != null) {
                methodMetaData.getHeaders().add(resolve(strings[0]), resolve(strings[1].trim()));
            }
        }
    }

    private void processMethodParametersAnnotations(MethodMetaData methodMetaData) {
        int parameters = methodMetaData.getMethod().getParameterCount();
        for (int i = 0; i < parameters; i++) {
            MethodParameter methodParameter = SynthesizingMethodParameter.forExecutable(methodMetaData.getMethod(), i);
            // TODO:
//            methodParameter.nestedIfOptional()?
            methodParameter.initParameterNameDiscovery(parameterNameDiscoverer);
            String parameterName = methodParameter.getParameterName();

            PathVariable pathVariable = methodParameter.getParameterAnnotation(PathVariable.class);
            if (pathVariable != null) {
                String name = hasText(pathVariable.value()) ? pathVariable.value() : parameterName;
                methodMetaData.addParameterDescription(name, i, Type.PathVariable);
            }

            RequestParam requestParam = methodParameter.getParameterAnnotation(RequestParam.class);
            if (requestParam != null) {
                String name = hasText(requestParam.value()) ? requestParam.value() : parameterName;
                methodMetaData.addParameterDescription(name, i, Type.RequestParam);
            }
        }

    }

    private void processMethodRequestMappingAnnotation(MethodMetaData methodMetaData) {
        RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(methodMetaData.getMethod(),
                RequestMapping.class);

        processHttpMethod(methodMetaData, requestMapping);
        processConsumes(methodMetaData, requestMapping);
        processProduces(methodMetaData, requestMapping);
        processPath(methodMetaData, requestMapping);

        processHeaders(methodMetaData, requestMapping);
    }

    private void processPath(MethodMetaData methodMetaData, RequestMapping requestMapping) {

        checkAtMostOne(methodMetaData.getMethod(), requestMapping.value(), "value");
        if (requestMapping.value().length > 0) {
            String pathValue = emptyToNull(requestMapping.value()[0]);
            if (pathValue != null) {
                pathValue = resolve(pathValue);
                // Append path from @RequestMapping if value is present on method
                methodMetaData.getPathSegments().add(pathValue);
            }
        }
    }

    private String resolve(String value) {
        if (StringUtils.hasText(value)) {
            return environment.resolvePlaceholders(value);
        }
        return value;
    }
}
