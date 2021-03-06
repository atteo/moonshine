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
package org.atteo.moonshine.shiro.simple;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import org.atteo.config.XmlDefaultValue;

/**
 * Single user account.
 */
public class Account {
	@XmlAttribute
	private String username;

	@XmlAttribute
	private String password;

	@XmlAttribute
	@XmlDefaultValue("false")
	private Boolean administrator;

	@XmlElementWrapper(name = "roles")
	@XmlElement(name = "role")
	private List<String> roles;

	public String getPassword() {
		return password;
	}

	public List<String> getRoles() {
		return roles;
	}

	public String getUsername() {
		return username;
	}

	public boolean isAdministrator() {
		return administrator;
	}
}
