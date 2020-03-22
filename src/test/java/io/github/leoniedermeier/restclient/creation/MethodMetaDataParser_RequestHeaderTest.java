package io.github.leoniedermeier.restclient.creation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import io.github.leoniedermeier.restclient.annotation.RestClient;
import io.github.leoniedermeier.restclient.creation.MethodMetaData.ParameterDesciption;
import io.github.leoniedermeier.restclient.creation.MethodMetaData.ParameterDesciption.Type;

class MethodMetaDataParser_RequestHeaderTest {

    @RestClient(url = "myUrl")
    interface RequestHeaderTestClient {

        @RequestMapping()
        void noRequestHeader(String myParameter);

        @RequestMapping
        void withRequestHeader(String myParameter, @RequestHeader String myRequestHeader);

        @RequestMapping
        void withRequestHeaderAndName(@RequestHeader(name = "myName") String myRequestHeader);

        @RequestMapping
        void withRequestHeaderRequiredFalse(@RequestHeader(required = false) String myRequestHeader);
    }

    private MethodMetaDataParser parser = new MethodMetaDataParser(new MockEnvironment());

    private Method getMethod(String methodName) {
        return ReflectionUtils.findMethod(RequestHeaderTestClient.class, methodName, null);
    }

    @Test
    void noRequestHeader() throws Exception {
        MethodMetaData methodMetaData = parser.parse(getMethod("noRequestHeader"));

        assertEquals(methodMetaData.getParameterDesciptions(Type.RequestHeader).size(), 0);
    }

    @Test
    void withRequestHeader() throws Exception {
        MethodMetaData methodMetaData = parser.parse(getMethod("withRequestHeader"));
        assertOneRequestHeaderWithName(methodMetaData, 1, "myRequestHeader", true);
    }

    @Test
    void withRequestHeaderAndName() throws Exception {
        MethodMetaData methodMetaData = parser.parse(getMethod("withRequestHeaderAndName"));
        assertOneRequestHeaderWithName(methodMetaData, 0, "myName", true);
    }

    @Test
    void withRequestHeaderRequiredFalse() throws Exception {
        MethodMetaData methodMetaData = parser.parse(getMethod("withRequestHeaderRequiredFalse"));
        assertOneRequestHeaderWithName(methodMetaData, 0, "myRequestHeader", false);
    }

    private static void assertOneRequestHeaderWithName(MethodMetaData methodMetaData, int parameterIndex,
            String expected, boolean required) {
        assertEquals(1, methodMetaData.getParameterDesciptions(Type.RequestHeader).size());
        ParameterDesciption parameterDesciption = methodMetaData.getParameterDesciptions(Type.RequestHeader).get(0);
        assertEquals(parameterIndex, parameterDesciption.getIndex());
        assertEquals(expected, parameterDesciption.getName());
        assertEquals(required, parameterDesciption.isRequired());
    }
}
