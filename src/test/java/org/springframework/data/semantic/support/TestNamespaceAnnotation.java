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
package org.springframework.data.semantic.support;

import static org.junit.Assert.assertEquals;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.junit.Test;
import org.springframework.data.semantic.mapping.SemanticPersistentEntity;
import org.springframework.data.semantic.mapping.SemanticPersistentProperty;
import org.springframework.data.semantic.model.NamespaceEntity;
import org.springframework.data.semantic.support.mapping.SemanticMappingContext;
import org.springframework.data.semantic.support.util.ValueUtils;

public class TestNamespaceAnnotation {
	
	private SemanticMappingContext mappingContext = new SemanticMappingContext((List<? extends Namespace>) new LinkedList<Namespace>(), new SimpleNamespace("", "urn:default:namespace:"), true);
	
	@SuppressWarnings("unchecked")
	private SemanticPersistentEntity<NamespaceEntity> pe = (SemanticPersistentEntity<NamespaceEntity>) mappingContext.getPersistentEntity(NamespaceEntity.class);
	
	private String namespace = "urn:test:namespace:";
	
	@Test
	public void testGetPredicate(){
		SemanticPersistentProperty pp = pe.getPersistentProperty("name");
		assertEquals(namespace+"name" , pp.getPredicate().stringValue());
	}
	
	@Test
	public void testGetType(){
		assertEquals(namespace+"NamespaceEntity", pe.getRDFType().stringValue());
	}

	@Test
	public void testAbsolutePredicate() {
		// make sure @Predicate() annotations with absolute IRI stay absolute and the same
		assertEquals(
				ValueUtils.createIRI("urn:really:absolute"),
				pe.getPersistentProperty("withAbsolutePredicate").getPredicate());
	}

	@Test
	public void testRelativePredicate() {
		assertEquals(
				ValueUtils.createIRI(namespace + "relative"),
				pe.getPersistentProperty("withRelativePredicate").getPredicate());
	}
}
