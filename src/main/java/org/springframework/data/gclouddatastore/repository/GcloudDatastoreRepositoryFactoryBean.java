/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.springframework.data.gclouddatastore.repository;

import java.io.Serializable;

import com.google.cloud.datastore.DatastoreOptions;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.util.Assert;

public class GcloudDatastoreRepositoryFactoryBean<T extends Repository<S, ID>, S, ID extends Serializable>
		extends RepositoryFactoryBeanSupport<T, S, ID> {

	private DatastoreOptions datastoreOptions;

	public GcloudDatastoreRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
		super(repositoryInterface);
	}

	@Override
	protected RepositoryFactorySupport createRepositoryFactory() {

		return new GcloudDatastoreRepositoryFactory(datastoreOptions);
	}

	public void setDatastoreOptions(DatastoreOptions datastoreOptions) {

		this.datastoreOptions = datastoreOptions;
	}

	@Override
	public void afterPropertiesSet() {

		Assert.notNull(datastoreOptions, "you must provide DatastoreOptions it cannot be null!");
		super.afterPropertiesSet();
	}
}
