/**
 * Copyright (C) 2014 Ontotext AD (info@ontotext.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.semantic.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.core.support.TransactionalRepositoryFactoryBeanSupport;
import org.springframework.data.semantic.core.SemanticOperationsCRUD;
import org.springframework.data.semantic.support.repository.FilteringSemanticRepositoryFactory;

import java.io.Serializable;

/**
 * Created by itrend on 4/27/15.
 */
public class FilteringSemanticRepositoryFactoryBean <T extends Repository<S, ID>, S, ID extends Serializable> extends
		TransactionalRepositoryFactoryBeanSupport<T, S, ID> {

	@Autowired
	private SemanticOperationsCRUD operations;


	@Override protected RepositoryFactorySupport doCreateRepositoryFactory() {
		return new FilteringSemanticRepositoryFactory(operations);
	}

	public SemanticOperationsCRUD getOperations() {
		return operations;
	}

	public void setOperations(SemanticOperationsCRUD operations) {
		this.operations = operations;
	}
}