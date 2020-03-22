package io.github.leoniedermeier.restclient.creation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import io.github.leoniedermeier.restclient.creation.MethodMetaData.ParameterDesciption.Type;

class MethodMetaData {

    static class ParameterDesciption {
        enum Type {
            PathVariable, RequestBody, RequestHeader, RequestParam
        }

        private final int index;
        private final String name;
        private final Type type;
        private final boolean required;

        /**
         * @param name  The name of the parameter.
         * @param index The index of the parameter
         * @param type  The type of the parameter.
         */
        public ParameterDesciption(String name, int index, boolean required, Type type) {
            super();

            this.name = name;
            this.index = index;
            this.required = required;
            this.type = type;
        }

        public int getIndex() {
            return index;
        }

        public String getName() {
            return name;
        }

        public Type getType() {
            return type;
        }

        public boolean isRequired() {
            return required;
        }
    }

    private final HttpHeaders headers = new HttpHeaders();

    private HttpMethod httpMethod;
    private Method method;
    private final List<ParameterDesciption> parameterDesciptions = new ArrayList<>();

    private List<String> pathSegments = new ArrayList<>();

    private String url;

    public MethodMetaData() {
        super();
    }

    public void addParameterDescription(String name, int index, boolean required, Type type) {
        parameterDesciptions.add(new ParameterDesciption(name, index, required, type));
    }

    /**
     * Returns the {@link HttpHeaders}.
     * 
     * @return The {@link HttpHeaders}, never <code>null</code>.
     */
    public HttpHeaders getHeaders() {
        return headers;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public Method getMethod() {
        return method;
    }

    /**
     * Returns an unmodifiable {@link List} of the {@link ParameterDesciption}.
     */
    public List<ParameterDesciption> getParameterDesciptions() {
        return parameterDesciptions;
    }

    public List<ParameterDesciption> getParameterDesciptions(Type type) {
        return parameterDesciptions.stream().filter(p -> p.type == type).collect(Collectors.toList());
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
