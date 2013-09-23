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
package org.atteo.moonshine.shiro;

import org.apache.shiro.realm.Realm;
import org.atteo.moonshine.ConfigurableService;

/**
 * Shiro realm service.
 *
 * <p>
 * Must bind {@link Realm}.
 * </p>
 */
public abstract class RealmService extends ConfigurableService {
}
