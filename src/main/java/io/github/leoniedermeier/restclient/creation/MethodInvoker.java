package io.github.leoniedermeier.restclient.creation;

import static io.github.leoniedermeier.restclient.creation.MethodMetaData.ParameterDesciption.Type.PathVariable;
import static io.github.leoniedermeier.restclient.creation.MethodMetaData.ParameterDesciption.Type.RequestParam;
import static java.util.stream.Collectors.toMap;

import java.net.URI;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;

import io.github.leoniedermeier.restclient.creation.MethodMetaData.ParameterDesciption;
import io.github.leoniedermeier.restclient.creation.MethodMetaData.ParameterDesciption.Type;

class MethodInvoker implements MethodInterceptor {

    private final RestOperations restOperations;
    private final MethodMetaData methodMetaData;

    public MethodInvoker(RestOperations restOperations, MethodMetaData methodMetaData) {
        super();
        this.restOperations = restOperations;
        this.methodMetaData = methodMetaData;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        HttpHeaders httpHeaders = buildHttpHeaders(methodMetaData, invocation);
        Object body = buildBody(methodMetaData, invocation);

        HttpEntity<?> requestEntity = new HttpEntity<>(body, httpHeaders);

        URI uri = buildUriString(methodMetaData, invocation);

        ParameterizedTypeReference<?> responseType = ParameterizedTypeReference
                .forType(invocation.getMethod().getGenericReturnType());

        ResponseEntity<?> result = restOperations.exchange(uri, methodMetaData.getHttpMethod(), requestEntity,
                responseType);
        // TODO: check for null
        return result.getBody();
    }

    private Object buildBody(MethodMetaData methodMetaData, MethodInvocation invocation) {
        return methodMetaData.getParameterDesciptions(Type.RequestBody).stream()
                .map(pd -> getArgumentValue(invocation, pd)).findAny().orElse(null);
    }

    private static HttpHeaders buildHttpHeaders(MethodMetaData methodMetaData, MethodInvocation invocation) {
        HttpHeaders headers = new HttpHeaders();
        headers.addAll(methodMetaData.getHeaders());
        methodMetaData.getParameterDesciptions(Type.RequestHeader).forEach(pd -> {
            Object value = getArgumentValue(invocation, pd);
            String stringValue = value != null ? value.toString() : null;
            headers.add(pd.getName(), stringValue);
        });
        return headers;
    }

    private static URI buildUriString(MethodMetaData methodMetaData, MethodInvocation invocation) {
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(methodMetaData.getUrl());

        methodMetaData.getPathSegments().forEach(uriComponentsBuilder::path);

        methodMetaData.getParameterDesciptions(RequestParam)
                .forEach(pd -> uriComponentsBuilder.queryParam(pd.getName(), getArgumentValue(invocation, pd)));

        Map<String, Object> uriVariables = methodMetaData.getParameterDesciptions(PathVariable).stream()
                .collect(toMap(pd -> pd.getName(), pd -> getArgumentValue(invocation, pd)));

        return uriComponentsBuilder.buildAndExpand(uriVariables).toUri();

    }

    private static Object getArgumentValue(MethodInvocation invocation, ParameterDesciption pd) {

        Object value = invocation.getArguments()[pd.getIndex()];
        if (value == null && pd.isRequired()) {
            throw new IllegalArgumentException("XXXXXX");
        }
        return value;
    }
}