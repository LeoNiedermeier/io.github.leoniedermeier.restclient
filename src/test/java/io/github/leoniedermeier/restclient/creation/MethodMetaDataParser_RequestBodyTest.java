package io.github.leoniedermeier.restclient.creation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.util.ReflectionUtils.getUniqueDeclaredMethods;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import io.github.leoniedermeier.restclient.annotation.RestClient;
import io.github.leoniedermeier.restclient.creation.MethodMetaData.ParameterDesciption;
import io.github.leoniedermeier.restclient.creation.MethodMetaData.ParameterDesciption.Type;

class MethodMetaDataParser_RequestBodyTest {

    @RestClient(url = "myUrl")
    interface RequestBodyTestClient {

        @RequestMapping()
        void noRequestBody(String myParameter);

        @RequestMapping
        void withRequestBody(String myParameter, @RequestBody Object myRequestBody);

        @RequestMapping
        void withMultipleRequestBody(@RequestBody String oneRequestBody, @RequestBody Object otherRequestBody);

        @RequestMapping
        void withRequestBodyRequiredFalse(@RequestBody(required = false) Object myRequestBody);
    }

    private MethodMetaDataParser parser = new MethodMetaDataParser(new MockEnvironment());

    private Method getMethod(String methodName) {
        return  ReflectionUtils.findMethod(RequestBodyTestClient.class, methodName, null);
    }

    @Test
    void noRequestBody() {
        MethodMetaData methodMetaData = parser.parse(getMethod("noRequestBody"));

        assertEquals(methodMetaData.getParameterDesciptions(Type.RequestBody).size(), 0);
    }

    @Test
    void withRequestBody() {
        MethodMetaData methodMetaData = parser.parse(getMethod("withRequestBody"));
        assertRequestBody(methodMetaData, 1, true);
    }

    @Test
    void withRequestHeaderAndName() {
        assertThrows(IllegalStateException.class, () -> parser.parse(getMethod("withMultipleRequestBody")));
    }

    @Test
    void withRequestBodyRequiredFalse() {
        MethodMetaData methodMetaData = parser.parse(getMethod("withRequestBodyRequiredFalse"));
        assertRequestBody(methodMetaData, 0, false);
    }

    private static void assertRequestBody(MethodMetaData methodMetaData, int parameterIndex, boolean required) {
        assertEquals(1, methodMetaData.getParameterDesciptions(Type.RequestBody).size());
        ParameterDesciption parameterDesciption = methodMetaData.getParameterDesciptions(Type.RequestBody).get(0);
        assertEquals(parameterIndex, parameterDesciption.getIndex());
        assertEquals(required, parameterDesciption.isRequired());
    }
}
