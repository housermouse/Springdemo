package com.apex.ams.config.server;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import java.util.Collections;

public class ConfigServerBootstrapApplicationListener implements
        ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

	public static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 4;

	private int order = DEFAULT_ORDER;

	private final PropertySource<?> propertySource = new MapPropertySource(
			"configServerClient", Collections.singletonMap(
					"ams.config.enabled", "true"));

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
		ConfigurableEnvironment environment = event.getEnvironment();
		if (!"true".equalsIgnoreCase(environment.resolvePlaceholders("${ams.config.enabled:false}"))) {
			if (!environment.getPropertySources().contains(this.propertySource.getName())) {
				environment.getPropertySources().addLast(this.propertySource);
			}
		}
	}

}
