package io.github.leoniedermeier.restclient.registration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.context.named.NamedContextFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

/**
 * A factory that creates instances of feign classes. It creates a Spring
 * ApplicationContext per client name, and extracts the beans that it needs from there.
 *
 * @author Spencer Gibb
 * @author Dave Syer
 */
public class RestClientContext extends NamedContextFactory<RestClientSpecification> {

	public RestClientContext() {
		super(RestClientDefaultConfiguration.class, "restClient", "restClient.client.name");
	}

}

@Configuration(proxyBeanMethods = false)
class RestClientDefaultConfiguration {
	// resttemplate
	
	@Bean
	@ConditionalOnMissingBean
	public RestOperations restClientRestTemplate() {
		return new RestTemplate();
	}
}