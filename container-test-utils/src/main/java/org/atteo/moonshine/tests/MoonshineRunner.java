/*
 * Copyright 2012 Atteo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.atteo.moonshine.tests;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.atteo.moonshine.Moonshine;
import org.junit.rules.MethodRule;
import org.junit.rules.TestRule;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;

/**
 * Runs the tests inside {@link Moonshine} container.
 *
 * <p>
 * You can configure the container by annotating the class with
 * {@link MoonshineConfiguration}.
 * </p>
 * <p>
 * The test class will be instantiated using global Guice injector of the
 * Moonshine container.
 * </p>
 */
public class MoonshineRunner extends BlockJUnit4ClassRunner {

	private MoonshineRule moonshineRule = null;

	private boolean requestPerClass = false;

	private List<String> selectedConfigs = Collections.emptyList();

	public MoonshineRunner(Class<?> klass) throws InitializationError {
		super(klass);
	}

	public MoonshineRunner(Class<?> klass, List<String> configs) throws InitializationError {
		this(klass);

		this.selectedConfigs = configs;
	}

	@Override
	protected Object createTest() throws Exception {
		return moonshineRule.getGlobalInjector().getInstance(getTestClass().getJavaClass());
	}

	@Override
	protected List<TestRule> classRules() {
		@SuppressWarnings("unchecked")
		Set<Class<?>> ancestorSet = (Set<Class<?>>) TypeToken.of(getTestClass().getJavaClass()).getTypes().rawTypes();
		List<Class<?>> ancestors = Lists.reverse(new ArrayList<>(ancestorSet));

		List<String> configs = new ArrayList<>();
		List<MoonshineConfigurator> configurators = new ArrayList<>();

		for (Class<?> ancestor : ancestors) {
			final MoonshineConfiguration annotation = ancestor.getAnnotation(MoonshineConfiguration.class);
			if (annotation == null) {
				continue;
			}

			for (String config : annotation.value()) {
				if (!config.startsWith("/")) {
					config = "/" + ancestor.getPackage().getName().replace(".", "/") + "/" + config;
				}
				configs.add(config);
			}

			if (annotation.oneRequestPerClass()) {
				requestPerClass = annotation.oneRequestPerClass();
			}

			Class<? extends MoonshineConfigurator> configuratorKlass = annotation.configurator();
			if (configuratorKlass != null && configuratorKlass != MoonshineConfigurator.class) {
				try {
					MoonshineConfigurator configurator = configuratorKlass.getConstructor().newInstance();
					configurators.add(configurator);
				} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
						| IllegalArgumentException | InvocationTargetException e) {
					throw new RuntimeException(e);
				}
			}

			if (annotation.autoConfiguration() || annotation.skipDefault() || !annotation.fromString().isEmpty()
					|| annotation.arguments().length != 0) {
				MoonshineConfigurator configurator = new MoonshineConfigurator() {
					@Override
					public void configureMoonshine(Moonshine.Builder builder) {
						if (annotation.autoConfiguration()) {
							builder.autoConfiguration();
						}
						if (annotation.skipDefault()) {
							builder.skipDefaultConfigurationFiles();
						}
						if (!annotation.fromString().isEmpty()) {
							builder.addConfigurationFromString(annotation.fromString());
						}

						builder.arguments(annotation.arguments());
					}
				};
				configurators.add(configurator);
			}
		}

		boolean useDefaultConfigs = configs.isEmpty() && configurators.isEmpty();

		configurators.add(new MoonshineConfigurator() {
			@Override
			public void configureMoonshine(Moonshine.Builder builder) {
				builder.addModule(new AbstractModule() {
					@Override
					protected void configure() {
						bind(new TypeLiteral<List<String>>() {
						}).annotatedWith(SelectedConfigs.class).toInstance(selectedConfigs);
					}
				});
			}
		});

		for (String externalConfig : selectedConfigs) {
			if (!externalConfig.startsWith("/")) {
				externalConfig = "/" + getTestClass().getJavaClass().getPackage().getName().replace(".", "/")
				    + "/" + externalConfig;
			}
			configs.add(externalConfig);
		}

		moonshineRule = new MoonshineRule(configurators, configs.toArray(new String[configs.size()]));
		moonshineRule.setApplicationName(getTestName());

		if (!useDefaultConfigs) {
			moonshineRule.skipDefaultTestConfigurationFiles();
		}

		List<TestRule> rules = super.classRules();
		if (requestPerClass) {
			rules.add(new RequestRule());
		}

		rules.add(moonshineRule);
		return rules;
	}

	@Override
	protected List<TestRule> getTestRules(Object target) {
		List<TestRule> rules = super.getTestRules(target);
		if (!requestPerClass) {
			rules.add(new RequestRule());
		}
		return rules;
	}

	@Override
	protected List<MethodRule> rules(Object target) {
		List<MethodRule> rules = super.rules(target);
		rules.add(moonshineRule.injectMembers(target));
		rules.add(new MockitoRule());
		return rules;
	}

	private String getTestName() {
		return getTestClass().getJavaClass().getSimpleName() + selectedConfigsString(selectedConfigs);
	}

	private String selectedConfigsString(List<String> selectedConfigs) {
		StringBuilder string = new StringBuilder();

		for (String selectedConfig : selectedConfigs) {
			string.append(".");
			string.append(new File(selectedConfig).getName().replace(".xml", ""));
		}

		return string.toString();
	}

}
