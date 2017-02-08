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
package org.springframework.data.semantic.support.repository;

import org.openrdf.model.IRI;
import org.springframework.data.repository.core.support.AbstractEntityInformation;

public class SemanticMetamodelEntityInformation<T> extends AbstractEntityInformation<T, IRI> {

	public SemanticMetamodelEntityInformation(Class<T> domainClass) {
		super(domainClass);
	}

	@Override
	public IRI getId(T entity) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class<IRI> getIdType() {
		return IRI.class;
	}

}
