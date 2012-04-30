/*
 * Copyright 2011 Atteo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.atteo.evo.hibernate;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;
import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;

import org.atteo.evo.classindex.ClassIndex;
import org.atteo.evo.database.DatabaseService;
import org.atteo.evo.services.TopLevelService;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.ejb.HibernatePersistence;
import org.hibernate.service.jta.platform.spi.JtaPlatform;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import com.google.inject.binder.ScopedBindingBuilder;
import com.google.inject.name.Names;

@XmlRootElement(name = "hibernate")
public class Hibernate extends TopLevelService {
	/**
	 * Select ID of the database to use.
	 * <p>
	 * Only needed if more than one is configured.
	 * </p>
	 */
	@XmlElement
	@XmlIDREF
	private DatabaseService database;

	/**
	 * Automatically initialize database schema.
	 * 
	 * <p>
	 * <ul>
	 * <li>validate: validate the schema, makes no changes to the database.</li>
	 * <li>update: update the schema.</li>
	 * <li>create: creates the schema, destroying previous data.</li>
	 * <li>create-drop: drop the schema at the end of the session.</li>
	 * </ul>
	 * Use evo-migrations in production setups.
	 * </p>
	 */
	@XmlElement
	private String initSchema = "validate";

	/**
	 * List of Hibernate plugins.
	 */
	@XmlElementRef
	@XmlElementWrapper(name = "plugins")
	private List<HibernatePlugin> plugins;

	/**
	 * Should Hibernate be loaded on first use.
	 */
	@XmlElement
	private boolean lazyLoading = false;

	private EntityManagerFactory factory;

	@Override
	public Module configure() {
		return new AbstractModule() {

			@Override
			protected void configure() {
				bind(JtaPlatform.class).to(CustomJtaPlatform.class).in(Scopes.SINGLETON);

				String id = getId();
				ScopedBindingBuilder binding;
				if (id == null) {
					binding = bind(EntityManagerFactory.class).toProvider(
							new EntityManagerFactoryProvider());
				} else {
					binding = bind(Key.get(EntityManagerFactory.class, Names.named(id))).toProvider(
							new EntityManagerFactoryProvider());
				}
				if (lazyLoading) {
					binding.in(Scopes.SINGLETON);
				} else {
					binding.asEagerSingleton();
				}
			}
		};
	}

	private class EntityManagerFactoryProvider implements Provider<EntityManagerFactory> {
		@Inject
		private Injector injector;

		@Inject
		private JtaPlatform jtaPlatform;

		@Override
		public EntityManagerFactory get() {
			String id = null;
			if (database != null)
				id = database.getId();

			final DataSource dataSource;
			if (id != null) {
				dataSource = injector.getInstance(Key.get(DataSource.class, Names.named(id)));
			} else {
				dataSource = injector.getInstance(DataSource.class);
			}

			PersistenceUnitInfo info = new PersistenceUnitInfo() {
				@Override
				public String getPersistenceUnitName() {
					String id = getId();
					if (id == null) {
						id = "default";
					}
					return id;
				}

				@Override
				public String getPersistenceProviderClassName() {
					return null;
				}

				@Override
				public PersistenceUnitTransactionType getTransactionType() {
					return PersistenceUnitTransactionType.JTA;
				}

				@Override
				public DataSource getJtaDataSource() {
					return dataSource;
				}

				@Override
				public DataSource getNonJtaDataSource() {
					return null;
				}

				@Override
				public List<String> getMappingFileNames() {
					return Collections.emptyList();
				}

				@Override
				public List<URL> getJarFileUrls() {
					return Collections.emptyList();
				}

				@Override
				public URL getPersistenceUnitRootUrl() {
					return null;
				}

				@Override
				public List<String> getManagedClassNames() {
					List<String> names = new ArrayList<String>();
					for (Class<?> klass : ClassIndex.getAnnotated(Entity.class)) {
						names.add(klass.getCanonicalName());
					}
					return names;
				}

				@Override
				public boolean excludeUnlistedClasses() {
					return true;
				}

				@Override
				public Properties getProperties() {
					Properties properties = new Properties();
					return properties;
				}

				@Override
				public ClassLoader getClassLoader() {
					return Hibernate.class.getClassLoader();
				}

				@Override
				public void addTransformer(ClassTransformer transformer) {
					throw new UnsupportedOperationException("Not supported yet.");
				}

				@Override
				public ClassLoader getNewTempClassLoader() {
					return getClassLoader();
				}

				@Override
				public SharedCacheMode getSharedCacheMode() {
					return SharedCacheMode.UNSPECIFIED;
				}

				@Override
				public ValidationMode getValidationMode() {
					return ValidationMode.AUTO;
				}

				@Override
				public String getPersistenceXMLSchemaVersion() {
					return "";
				}
			};

			PersistenceProvider provider = new HibernatePersistence();

			Map<String, Object> map = new HashMap<String, Object>();
			if (plugins != null) {
				for (HibernatePlugin plugin : plugins) {
					map.putAll(plugin.getProperties());
				}
			}
			map.put(AvailableSettings.JTA_PLATFORM, jtaPlatform);
			map.put(AvailableSettings.HBM2DDL_AUTO, initSchema);
			factory = provider.createContainerEntityManagerFactory(info, map);
			return factory;
		}
	}

	@Override
	public void stop() {
		if (factory != null && factory.isOpen()) {
			factory.close();
		}
	}
}
