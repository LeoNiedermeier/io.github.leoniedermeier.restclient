package io.github.leoniedermeier.restclient.jsonplaceholder;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.leoniedermeier.restclient.creation.RestClientFactoryBean;

class JSONPlaceHolderClientTest {

    @Test
    void test() throws Exception {
        RestTemplate restTemplate = new RestTemplate();

        RestClientFactoryBean factoryBean = new RestClientFactoryBean();
        factoryBean.setType(JSONPlaceHolderClient.class);
        factoryBean.setEnvironment(new MockEnvironment());
        factoryBean.setRestOperations(restTemplate);

        JSONPlaceHolderClient client = (JSONPlaceHolderClient) factoryBean.getObject();
        Object postById = client.getPostsByUserId("1");
        // client.getPostById(2L);
        System.out.println("=========");
        ObjectMapper mapper = new ObjectMapper();
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(postById));
    }

}
