package io.github.leoniedermeier.restclient.creation;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import io.github.leoniedermeier.restclient.annotation.RestClient;

class MethodMetaDataParser_RequestMappingTest {

    static class HeadersTest {
        @RestClient(url = "myUrl")
        interface HeadersTestClient {

            @RequestMapping(headers = { "x=1", "y=2", "z=3" })
            void headers();

            @RequestMapping(headers = { "x!=1", "x=2" })
            void ignoreHeader();

            @RequestMapping(headers = { "x=1", "x=2" })
            void multipleHeaderValues();

            @RequestMapping(headers = { "${x}=${y}" })
            void resolveHeaderValues();

        }

        private MethodMetaDataParser parser = new MethodMetaDataParser(new MockEnvironment());

        @Test
        void headers() throws Exception {
            MethodMetaData methodMetaData = parser.parse(HeadersTestClient.class.getMethod("headers"));

            assertSingleHeader(methodMetaData, "x", "1");
            assertSingleHeader(methodMetaData, "y", "2");
            assertSingleHeader(methodMetaData, "z", "3");
        }

        @Test
        void ignoreHeader() throws Exception {
            MethodMetaData methodMetaData = parser.parse(HeadersTestClient.class.getMethod("ignoreHeader"));
            assertSingleHeader(methodMetaData, "x", "2");
        }

        @Test
        void multipleHeaderValues() throws Exception {
            MethodMetaData methodMetaData = parser.parse(HeadersTestClient.class.getMethod("multipleHeaderValues"));

            assertMultipleHeaders(methodMetaData, "x", "1", "2");
        }

        @Test
        void resolveHeaderValues() throws Exception {
            MockEnvironment environment = new MockEnvironment();
            environment.setProperty("x", "x_resolved");
            environment.setProperty("y", "y_resolved");
            MethodMetaDataParser methodMetaDataParser = new MethodMetaDataParser(environment);
            MethodMetaData methodMetaData = methodMetaDataParser
                    .parse(HeadersTestClient.class.getMethod("resolveHeaderValues"));
            assertSingleHeader(methodMetaData, "x_resolved", "y_resolved");
        }
    }

    static class HttpMethodTest {

        @RestClient(url = "myUrl")
        interface HttpMethodTestClient {

            @RequestMapping()
            void noHttpMethod();

            @RequestMapping(method = RequestMethod.POST)
            void withHttpMethod();

            @RequestMapping(method = { RequestMethod.POST, RequestMethod.DELETE })
            void withMultipleHttpMethods();
        }

        private MethodMetaDataParser parser = new MethodMetaDataParser(new MockEnvironment());

        @Test
        void noHttpMethod() throws Exception {
            MethodMetaData methodMetaData = parser.parse(HttpMethodTestClient.class.getMethod("noHttpMethod"));

            assertEquals(HttpMethod.GET, methodMetaData.getHttpMethod());
        }

        @Test
        void withHttpMethod() throws Exception {
            MethodMetaData methodMetaData = parser.parse(HttpMethodTestClient.class.getMethod("withHttpMethod"));

            assertEquals(HttpMethod.POST, methodMetaData.getHttpMethod());
        }

        @Test
        void withMultipleHttpMethods() throws Exception {
            assertThrows(IllegalStateException.class,
                    () -> parser.parse(HttpMethodTestClient.class.getMethod("withMultipleHttpMethods")));
        }

    }

    static class PathTest {

        @RestClient(url = "myUrl")
        @RequestMapping(path = "pathOfClass")
        interface PathTestClient {

            @RequestMapping(path = { "path_1", "path_2" })
            void multiplePaths();

            @RequestMapping()
            void noMethodPath();

            @RequestMapping(path = "pathOfMethod")
            void path();

        }

        private MethodMetaDataParser parser = new MethodMetaDataParser(new MockEnvironment());

        @Test
        void multiplePaths() throws Exception {
            assertThrows(IllegalStateException.class,
                    () -> parser.parse(PathTestClient.class.getMethod("multiplePaths")));
        }

        @Test
        void noMethodPath() throws Exception {
            MethodMetaData methodMetaData = parser.parse(PathTestClient.class.getMethod("noMethodPath"));

            assertEquals(asList("pathOfClass"), methodMetaData.getPathSegments());
        }

        @Test
        void path() throws Exception {
            MethodMetaData methodMetaData = parser.parse(PathTestClient.class.getMethod("path"));

            assertEquals(asList("pathOfClass", "pathOfMethod"), methodMetaData.getPathSegments());

        }

    }

    private static void assertMultipleHeaders(MethodMetaData methodMetaData, String headerName,
            String... headerValues) {
        assertEquals(Arrays.asList(headerValues), methodMetaData.getHeaders().get(headerName));
    }

    private static void assertSingleHeader(MethodMetaData methodMetaData, String headerName, String headerValue) {
        assertEquals(singletonList(headerValue), methodMetaData.getHeaders().get(headerName));
    }
}
