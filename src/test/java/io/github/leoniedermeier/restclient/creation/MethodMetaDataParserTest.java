package io.github.leoniedermeier.restclient.creation;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.util.ReflectionUtils.getUniqueDeclaredMethods;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import io.github.leoniedermeier.restclient.annotation.RestClient;
import io.github.leoniedermeier.restclient.creation.MethodMetaData.ParameterDesciption.Type;

class MethodMetaDataParserTest {

	static class ConsumesTest {

		@RestClient(url = "myUrl")
		interface ConsumesTestClient {

			@RequestMapping(consumes = "consumes")
			void consumes();

			@RequestMapping(consumes = { "consumes_1", "consumes_2" })
			void multipleConsumes();

			@RequestMapping()
			void noConsumes();

		}

		private MethodMetaDataParser parser = new MethodMetaDataParser(new MockEnvironment());

		@Test
		void consumes() throws Exception {
			MethodMetaData methodMetaData = parser.parse(ConsumesTestClient.class.getMethod("consumes"));

			assertSingleHeader(methodMetaData, HttpHeaders.CONTENT_TYPE, "consumes");
		}

		@Test
		void multipleConsumes() throws Exception {
			MethodMetaData methodMetaData = parser.parse(ConsumesTestClient.class.getMethod("multipleConsumes"));

			assertSingleHeader(methodMetaData, HttpHeaders.CONTENT_TYPE, "consumes_1");

		}

		@Test
		void noConsumes() throws Exception {
			MethodMetaData methodMetaData = parser.parse(ConsumesTestClient.class.getMethod("noConsumes"));

			assertNoHeader(methodMetaData, HttpHeaders.CONTENT_TYPE);
		}

	}

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

	static class PathVariableTest {
		@RestClient(url = "myUrl")
		interface PathVariableTestClient {

			@RequestMapping()
			void noPathVariable(String myParameter);

			@RequestMapping
			void withPathVariable(String myParameter, @PathVariable String myPathVariable);

			@RequestMapping
			void withPathVariableAndName(@PathVariable(name = "myName") String myPathVariable);
		}

		private MethodMetaDataParser parser = new MethodMetaDataParser(new MockEnvironment());

		private Method getMethod(String string) {
			return getUniqueDeclaredMethods(PathVariableTestClient.class, m -> string.equals(m.getName()))[0];
		}

		@Test
		void noPathVariable() throws Exception {
			MethodMetaData methodMetaData = parser.parse(getMethod("noPathVariable"));

			assertEquals(methodMetaData.getParameterDesciptions(Type.PathVariable).size(), 0);
		}

		@Test
		void withPathVariable() throws Exception {
			MethodMetaData methodMetaData = parser.parse(getMethod("withPathVariable"));

			assertEquals(methodMetaData.getParameterDesciptions(Type.PathVariable).size(), 1);
			assertEquals(methodMetaData.getParameterDesciptions(Type.PathVariable).get(0).getIndex(), 1);
			assertEquals(methodMetaData.getParameterDesciptions(Type.PathVariable).get(0).getName(), "myPathVariable");
		}

		@Test
		void withPathVariableAndName() throws Exception {
			MethodMetaData methodMetaData = parser.parse(getMethod("withPathVariableAndName"));

			assertEquals(1, methodMetaData.getParameterDesciptions(Type.PathVariable).size());
			assertEquals(0, methodMetaData.getParameterDesciptions(Type.PathVariable).get(0).getIndex());
			assertEquals("myName", methodMetaData.getParameterDesciptions(Type.PathVariable).get(0).getName());
		}

	}

	static class ProducesTest {
		@RestClient(url = "myUrl")
		interface ProducesTestClient {

			@RequestMapping(produces = { "produces_1", "produces_2" })
			void multipleProduces();

			@RequestMapping(produces = { "" })
			void noProduces();

			@RequestMapping(produces = "produces")
			void produces();

		}

		private MethodMetaDataParser parser = new MethodMetaDataParser(new MockEnvironment());

		@Test
		void multipleProduces() throws Exception {
			MethodMetaData methodMetaData = parser.parse(ProducesTestClient.class.getMethod("multipleProduces"));

			assertSingleHeader(methodMetaData, HttpHeaders.ACCEPT, "produces_1");

		}

		@Test
		void noProduces() throws Exception {
			MethodMetaData methodMetaData = parser.parse(ProducesTestClient.class.getMethod("noProduces"));

			assertNoHeader(methodMetaData, HttpHeaders.ACCEPT);
		}

		@Test
		void produces() throws Exception {
			MethodMetaData methodMetaData = parser.parse(ProducesTestClient.class.getMethod("produces"));

			assertSingleHeader(methodMetaData, HttpHeaders.ACCEPT, "produces");
		}

	}

	private static void assertMultipleHeaders(MethodMetaData methodMetaData, String headerName,
			String... headerValues) {
		assertEquals(Arrays.asList(headerValues), methodMetaData.getHeaders().get(headerName));
	}

	private static void assertNoHeader(MethodMetaData methodMetaData, String headerName) {
		assertNull(methodMetaData.getHeaders().get(headerName));
	}

	private static void assertSingleHeader(MethodMetaData methodMetaData, String headerName, String headerValue) {
		assertEquals(singletonList(headerValue), methodMetaData.getHeaders().get(headerName));
	}
}
