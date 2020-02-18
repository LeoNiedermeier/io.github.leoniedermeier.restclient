package io.github.leoniedermeier.restclient.registration;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestOperations;

import io.github.leoniedermeier.restclient.creation.RestClientFactoryBean;

public class RestClientFactoryBean2 implements FactoryBean<Object>, EnvironmentAware, ApplicationContextAware {

	private Environment environment;
	private ApplicationContext applicationContext;
	private Class<?> type;
	 

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public Object getObject() throws Exception {
		// in autoconfiguration gesetet, siehe FeignAutoConfiguration
		RestClientContext context = this.applicationContext.getBean(RestClientContext.class);
		RestClientFactoryBean x = new RestClientFactoryBean();
		x.setEnvironment(environment);
		x.setType(type);
		RestOperations restOperations = context.getInstance(name, RestOperations.class);
		x.setRestOperations(restOperations);
		return null;
	}

	@Override
	public Class<?> getObjectType() {
		return this.type;
	}

	public void setType(Class<?> type) {
		this.type = type;
	}
 
	
	private String name;
	public void setName(String name) {
		this.name = name;
	}
}
