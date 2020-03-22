package io.github.leoniedermeier.restclient.creation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.util.ReflectionUtils.getUniqueDeclaredMethods;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import io.github.leoniedermeier.restclient.annotation.RestClient;
import io.github.leoniedermeier.restclient.creation.MethodMetaData.ParameterDesciption;
import io.github.leoniedermeier.restclient.creation.MethodMetaData.ParameterDesciption.Type;
import io.github.leoniedermeier.restclient.creation.MethodMetaDataParser_RequestBodyTest.RequestBodyTestClient;

class MethodMetaDataParser_PathVariableTest {

    @RestClient(url = "myUrl")
    interface PathVariableTestClient {

        @RequestMapping()
        void noPathVariable(String myParameter);

        @RequestMapping
        void withPathVariable(String myParameter, @PathVariable String myPathVariable);

        @RequestMapping
        void withPathVariableAndName(@PathVariable(name = "myName") String myPathVariable);

        @RequestMapping
        void withPathVariableRequiredFalse(@PathVariable(required = false) String myPathVariable);
    }

    private MethodMetaDataParser parser = new MethodMetaDataParser(new MockEnvironment());

    private Method getMethod(String methodName) {
        return  ReflectionUtils.findMethod(PathVariableTestClient.class, methodName, null);
    }

    private List<ParameterDesciption> getParameterDescriptionsForPathVariable(MethodMetaData methodMetaData) {
        return methodMetaData.getParameterDesciptions(Type.PathVariable);
    }

    @Test
    void noPathVariable() {
        MethodMetaData methodMetaData = parser.parse(getMethod("noPathVariable"));

        assertEquals(0, getParameterDescriptionsForPathVariable(methodMetaData).size());
    }

    @Test
    void withPathVariable() {
        MethodMetaData methodMetaData = parser.parse(getMethod("withPathVariable"));

        assertOnePathVariableWithName(methodMetaData, 1, "myPathVariable", true);
    }

    @Test
    void withPathVariableAndName() {
        MethodMetaData methodMetaData = parser.parse(getMethod("withPathVariableAndName"));

        assertOnePathVariableWithName(methodMetaData, 0, "myName", true);
    }

    @Test
    void withPathVariableRequiredFalse() {
        MethodMetaData methodMetaData = parser.parse(getMethod("withPathVariableRequiredFalse"));

        assertOnePathVariableWithName(methodMetaData, 0, "myPathVariable", false);
    }

    private static void assertOnePathVariableWithName(MethodMetaData methodMetaData, int parameterIndex, String name,
            boolean required) {
        assertEquals(1, methodMetaData.getParameterDesciptions(Type.PathVariable).size());
        ParameterDesciption parameterDesciption = methodMetaData.getParameterDesciptions(Type.PathVariable).get(0);
        assertEquals(parameterIndex, parameterDesciption.getIndex());
        assertEquals(name, parameterDesciption.getName());
        assertEquals(required, parameterDesciption.isRequired());
    }
}
