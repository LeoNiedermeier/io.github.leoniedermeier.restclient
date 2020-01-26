package io.github.leoniedermeier.restclient.annotation;

import java.lang.reflect.Parameter;
import java.net.URI;
import java.util.HashMap;
import java.util.List;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.leoniedermeier.restclient.annotation.MethodMetaData.ParameterDesciption;
import io.github.leoniedermeier.restclient.annotation.MethodMetaData.ParameterDesciption.Type;
import io.github.leoniedermeier.restclient.annotation.xyz.JSONPlaceHolderClient;

class JSONPlaceHolderClientTest {

    @Test
    void test() throws Exception {
        RestTemplate restTemplate = new RestTemplate();

        RestClientFactoryBean factoryBean = new RestClientFactoryBean();
        factoryBean.setType(JSONPlaceHolderClient.class);
        factoryBean.setEnvironment(new MockEnvironment());
        factoryBean.setRestTemplate(restTemplate);

        JSONPlaceHolderClient client = (JSONPlaceHolderClient) factoryBean.getObject();
        Object postById = client.getPostsByUserId("1");
        // client.getPostById(2L);
        System.out.println("=========");
        ObjectMapper mapper = new ObjectMapper();
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(postById));
    }

}
