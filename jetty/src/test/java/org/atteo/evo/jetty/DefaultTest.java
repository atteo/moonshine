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
package org.atteo.evo.jetty;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import javax.inject.Inject;

import org.atteo.evo.tests.ServicesTest;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

public class DefaultTest extends ServicesTest {
	@Inject
	JettyConnectionDetails connectionDetails;

	@Test
	public void dummy() {
	}

	@Test
	public void testServlet() throws MalformedURLException, IOException {
		URL url = new URL("http", "localhost", connectionDetails.getPort(), "/servlet");
		try (InputStream stream = url.openStream()) {
			String result = CharStreams.toString(new InputStreamReader(stream, Charsets.UTF_8));
			assertEquals("hello", result);
		}
	}
}
