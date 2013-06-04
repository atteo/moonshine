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
package org.atteo.evo.jetty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.atteo.evo.webserver.WebServerAddress;
import org.eclipse.jetty.server.AbstractNetworkConnector;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.name.Names;

/**
 * This connector uses efficient NIO buffers with a non blocking threading model.
 * Direct NIO buffers are used and threads are only allocated to connections with requests.
 * Synchronization is used to simulate blocking for the servlet API, and any unflushed content
 * at the end of request handling is written asynchronously.
 *
 * This connector is best used when there are a many connections that have idle periods.
 *
 * @see ServerConnector
 */
@XmlRootElement(name = "serverconnector")
public class ServerConnectorConfig extends AbstractNetworkConnectorConfig {
	/**
	 * List of connection factories.
	 */
	@XmlElementWrapper(name = "connections")
	@XmlElementRef
	private ConnectionFactoryConfig[] connections = new ConnectionFactoryConfig[] {
		new HttpConnectionFactoryConfig()
	};

	/**
	 * Register {@link WebServerAddress} in Guice with port and hostname assigned to this connector.
	 */
	@XmlElement
	private boolean provideAddress = false;

	@Override
	public AbstractNetworkConnector createConnector(Server server) {
		ConnectionFactory[] connectionFactories = new ConnectionFactory[connections.length];
		int i = 0;
		for (ConnectionFactoryConfig connectionConfig : connections) {
			connectionFactories[i] = connectionConfig.getConnectionFactory();
			i++;
		}
		return new ServerConnector(server, connectionFactories);
	}

	@Override
	public Module configure() {
		return new AbstractModule() {
			@Override
			protected void configure() {
				if (provideAddress) {
					if (getId() == null) {
						bind(WebServerAddress.class).toInstance(getWebServerAddress());
					} else {
						bind(WebServerAddress.class).annotatedWith(Names.named(getId()))
								.toInstance(getWebServerAddress());
					}
				}
			}
		};
	}

	private WebServerAddress getWebServerAddress() {
		return new WebServerAddress() {
			@Override
			public int getPort() {
				return ServerConnectorConfig.this.getPort();
			}

			@Override
			public String getHost() {
				return ServerConnectorConfig.this.getHost();
			}

			@Override
			public String getUrl() {
				String host = ServerConnectorConfig.this.getHost();
				if (host == null) {
					host = "localhost";
				}
				return connections[0].getProtocolString() + "://" + host + ":" + ServerConnectorConfig.this.getPort();
			}
		};
	}
}
