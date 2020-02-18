package io.github.leoniedermeier.restclient.registration;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

public class RestClientRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {

	private Environment environment;
	private ResourceLoader resourceLoader;

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
		registerDefaultConfiguration(metadata, registry);
		registerClients(metadata, registry);
	}

	private void registerDefaultConfiguration(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
		Map<String, Object> defaultAttrs = metadata.getAnnotationAttributes(EnableRestClients.class.getName(), true);

		if (defaultAttrs != null && defaultAttrs.containsKey("defaultConfiguration")) {
			String name = "default."
					+ (metadata.hasEnclosingClass() ? metadata.getEnclosingClassName() : metadata.getClassName());
			registerClientConfiguration(registry, name, defaultAttrs.get("defaultConfiguration"));
		}
	}

	private void registerClients(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {

		Map<String, Object> attrs = metadata.getAnnotationAttributes(EnableFeignClients.class.getName());
		final Class<?>[] clients = attrs == null ? null : (Class<?>[]) attrs.get("clients");
		if (clients == null || clients.length == 0) {
			abc_1(metadata, registry);
		} else {
			abc_2(registry, clients);
		}

	}

	private void abc_2(BeanDefinitionRegistry registry, final Class<?>[] clients) {
		final Set<String> clientClasses = stream(clients).map(Class::getCanonicalName).collect(toSet());

		TypeFilter filter = (MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) -> {
			String cleaned = metadataReader.getClassMetadata().getClassName().replaceAll("\\$", ".");
			return clientClasses.contains(cleaned);

		};

		AnnotationTypeFilter annotationTypeFilter = new AnnotationTypeFilter(RestClient.class);

		ClassPathScanningCandidateComponentProvider scanner = getScanner();
		scanner.setResourceLoader(this.resourceLoader);
		scanner.addIncludeFilter(
				(metadataReader, metadataReaderFactory) -> filter.match(metadataReader, metadataReaderFactory)
						&& annotationTypeFilter.match(metadataReader, metadataReaderFactory));

		Set<String> basePackages = stream(clients).map(Class::getPackageName).collect(toSet());
		xyz(registry, scanner, basePackages);
	}

	private void abc_1(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
		ClassPathScanningCandidateComponentProvider scanner = getScanner();
		scanner.setResourceLoader(this.resourceLoader);
		scanner.addIncludeFilter(new AnnotationTypeFilter(RestClient.class));
		Set<String> basePackages = getBasePackages(metadata);
		xyz(registry, scanner, basePackages);
	}

	private void xyz(BeanDefinitionRegistry registry, ClassPathScanningCandidateComponentProvider scanner,
			Set<String> basePackages) {
		basePackages.stream().flatMap(p -> scanner.findCandidateComponents(p).stream())
				.filter(AnnotatedBeanDefinition.class::isInstance).map(AnnotatedBeanDefinition.class::cast)
				.forEach(beanDefinition -> {
					AnnotationMetadata annotationMetadata = beanDefinition.getMetadata();
					Assert.isTrue(annotationMetadata.isInterface(),
							"@RestClient can only be specified on an interface");

					Map<String, Object> attributes = annotationMetadata
							.getAnnotationAttributes(RestClient.class.getCanonicalName());

					String name = getClientName(attributes);
					registerClientConfiguration(registry, name, attributes.get("configuration"));

					registerRestClient(registry, annotationMetadata, attributes);

				});
	}

	private void registerRestClient(BeanDefinitionRegistry registry, AnnotationMetadata annotationMetadata,
			Map<String, Object> attributes) {
		String className = annotationMetadata.getClassName();
		BeanDefinitionBuilder definition = BeanDefinitionBuilder.genericBeanDefinition(RestClientFactoryBean.class);

		definition.addPropertyValue("url", getUrl(attributes));
		definition.addPropertyValue("path", getPath(attributes));
		String name = getName(attributes);
		definition.addPropertyValue("name", name);

		definition.addPropertyValue("type", className);
		definition.addPropertyValue("decode404", attributes.get("decode404"));
		definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);

		AbstractBeanDefinition beanDefinition = definition.getBeanDefinition();

		boolean primary = (Boolean) attributes.get("primary"); // has a default, won't be null
		beanDefinition.setPrimary(primary);

		BeanDefinitionHolder holder = new BeanDefinitionHolder(beanDefinition, className);
		BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
	}

	String getName(Map<String, Object> attributes) {
		String name = (String) attributes.get("name");

		if (!StringUtils.hasText(name)) {
			name = (String) attributes.get("value");
		}
		name = resolve(name);
		return name;
	}

	private String resolve(String value) {
		if (StringUtils.hasText(value)) {
			return this.environment.resolvePlaceholders(value);
		}
		return value;
	}

	private String getUrl(Map<String, Object> attributes) {
		String url = resolve((String) attributes.get("url"));
		return getUrl(url);
	}

	private String getPath(Map<String, Object> attributes) {
		String path = resolve((String) attributes.get("path"));
		return getPath(path);
	}

	static String getUrl(String url) {
		if (StringUtils.hasText(url) && !(url.startsWith("#{") && url.contains("}"))) {
			if (!url.contains("://")) {
				url = "http://" + url;
			}
			try {
				new URL(url);
			} catch (MalformedURLException e) {
				throw new IllegalArgumentException(url + " is malformed", e);
			}
		}
		return url;
	}

	static String getPath(String path) {
		if (StringUtils.hasText(path)) {
			path = path.trim();
			if (!path.startsWith("/")) {
				path = "/" + path;
			}
			path = StringUtils.trimTrailingCharacter(path, '/');
		}
		return path;
	}

	protected ClassPathScanningCandidateComponentProvider getScanner() {
		return new ClassPathScanningCandidateComponentProvider(false, this.environment) {
			protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
				return beanDefinition.getMetadata().isIndependent() && !beanDefinition.getMetadata().isAnnotation();
			}
		};
	}

	protected Set<String> getBasePackages(AnnotationMetadata importingClassMetadata) {
		Map<String, Object> attributes = importingClassMetadata
				.getAnnotationAttributes(EnableRestClients.class.getCanonicalName());

		Set<String> basePackages = new HashSet<>();
		
		Arrays.stream((String[]) attributes.get("value")).filter(StringUtils::hasText).forEach(basePackages::add);
		Arrays.stream((String[]) attributes.get("basePackages")).filter(StringUtils::hasText)
				.forEach(basePackages::add);
		Arrays.stream((Class[]) attributes.get("basePackageClasses")).map(ClassUtils::getPackageName)
				.forEach(basePackages::add);

		if (basePackages.isEmpty()) {
			basePackages.add(ClassUtils.getPackageName(importingClassMetadata.getClassName()));
		}
		return basePackages;
	}

	private String getClientName(Map<String, Object> client) {
		if (client == null) {
			return null;
		}
		String value = getName(client);

		if (StringUtils.hasText(value)) {
			return value;
		}

		throw new IllegalStateException(
				"Either 'name' or 'value' must be provided in @" + RestClient.class.getSimpleName());
	}

	private void registerClientConfiguration(BeanDefinitionRegistry registry, String name, Object configuration) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(RestClientSpecification.class);
		builder.addConstructorArgValue(name);
		builder.addConstructorArgValue(configuration);
		registry.registerBeanDefinition(name + "." + RestClientSpecification.class.getSimpleName(),
				builder.getBeanDefinition());
	}

}
