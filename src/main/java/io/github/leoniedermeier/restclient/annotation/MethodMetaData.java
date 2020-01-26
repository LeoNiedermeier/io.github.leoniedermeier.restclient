package io.github.leoniedermeier.restclient.annotation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import io.github.leoniedermeier.restclient.annotation.MethodMetaData.ParameterDesciption.Type;

public class MethodMetaData {

    public static class ParameterDesciption {
        enum Type {
            PathVariable, RequestBody, RequestHeader, RequestParam
        }

        int index;
        String name;
        Type type;

        public ParameterDesciption(String name, int index, Type type) {
            super();
            this.name = name;
            this.index = index;
            this.type = type;
        }

    }

    private final HttpHeaders headers = new HttpHeaders();

    private HttpMethod httpMethod;
    private Method method;
    private final List<ParameterDesciption> parameterDesciptions = new ArrayList<MethodMetaData.ParameterDesciption>();

    private List<String> pathSegments = new ArrayList<>();

    private String url;

    public void addParameterDescription(String name, int index, Type type) {
        parameterDesciptions.add(new ParameterDesciption(name, index, type));
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public Method getMethod() {
        return method;
    }

    public List<ParameterDesciption> getParameterDesciptions() {
        return parameterDesciptions;
    }
    
    public Stream<ParameterDesciption> getParameterDesciptions(Type type) {
        return parameterDesciptions.stream().filter(p -> p.type == type);
    }

    public List<String> getPathSegments() {
        return pathSegments;
    }

    public String getUrl() {
        return url;
    }

    public void setHttpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
