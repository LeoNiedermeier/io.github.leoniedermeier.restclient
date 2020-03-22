package io.github.leoniedermeier.restclient.creation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestOperations;

import io.github.leoniedermeier.restclient.annotation.RestClient;

class MethodInvokerTest {

    private RestOperations restOperations;
    private MethodInvokerTestClient client;

    @RestClient(url = "http://my-uri")
    interface MethodInvokerTestClient {

        @RequestMapping
        String getSimple();

        @RequestMapping(path = "/with/{id}/and/{other}")
        String getWithPathVariable(@PathVariable(name = "id") String id, Object xyz, @PathVariable String other);

        @RequestMapping
        String withBody(@RequestBody String body);
    }

    @BeforeEach
    void setUp() throws Exception {
        restOperations = Mockito.mock(RestOperations.class);

        RestClientFactoryBean factoryBean = new RestClientFactoryBean();
        factoryBean.setType(MethodInvokerTestClient.class);
        factoryBean.setEnvironment(new MockEnvironment());
        factoryBean.setRestOperations(restOperations);
        client = (MethodInvokerTestClient) factoryBean.getObject();

    }

    @Test
    void invoke_getSimple() throws Throwable {

        Mockito.when(restOperations.exchange(new URI("http://my-uri"), HttpMethod.GET, new HttpEntity<Object>(null),
                ParameterizedTypeReference.forType(String.class)))
                .thenReturn(new ResponseEntity<Object>("SIMPLE", HttpStatus.OK));

        Object result = client.getSimple();
        assertEquals("SIMPLE", result);
    }

    @Test
    void invoke_getWithPathVariable() throws Throwable {

        Mockito.when(restOperations.exchange(new URI("http://my-uri/with/myId/and/myOther"), HttpMethod.GET,
                new HttpEntity<Object>(null), ParameterizedTypeReference.forType(String.class)))
                .thenReturn(new ResponseEntity<Object>("SUCCESS", HttpStatus.OK));

        Object result = client.getWithPathVariable("myId", "xyz", "myOther");
        assertEquals("SUCCESS", result);
    }

    @Test
    void invoke_withBody() throws Throwable {

        Mockito.when(restOperations.exchange(new URI("http://my-uri"), HttpMethod.GET, new HttpEntity<Object>("myBody"),
                ParameterizedTypeReference.forType(String.class)))
                .thenReturn(new ResponseEntity<Object>("SUCCESS", HttpStatus.OK));

        Object result = client.withBody("myBody");
        assertEquals("SUCCESS", result);
    }
}
