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
package org.springframework.data.semantic.support.mapping;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AbstractPersistentProperty;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.semantic.annotation.Context;
import org.springframework.data.semantic.annotation.Datatype;
import org.springframework.data.semantic.annotation.Fetch;
import org.springframework.data.semantic.annotation.Language;
import org.springframework.data.semantic.annotation.Language.Languages;
import org.springframework.data.semantic.annotation.Optional;
import org.springframework.data.semantic.annotation.Predicate;
import org.springframework.data.semantic.annotation.RelatedTo;
import org.springframework.data.semantic.annotation.ResourceId;
import org.springframework.data.semantic.mapping.MappingPolicy;
import org.springframework.data.semantic.mapping.SemanticPersistentEntity;
import org.springframework.data.semantic.mapping.SemanticPersistentProperty;
import org.springframework.data.semantic.support.Direction;
import org.springframework.data.semantic.support.MappingPolicyImpl;
import org.springframework.data.semantic.support.util.ValueUtils;
import org.springframework.util.StringUtils;

/**
 * 
 * @author konstantin.pentchev
 *
 */

public class SemanticPersistentPropertyImpl extends
		AbstractPersistentProperty<SemanticPersistentProperty> implements
		SemanticPersistentProperty {

	private Map<Class<? extends Annotation>, ? extends Annotation> annotations;
	private boolean isIdProperty;
	private SemanticMappingContext mappingContext;
	private String aliasPredicate;
	private String bindingName;

	public SemanticPersistentPropertyImpl(Field field,
			PropertyDescriptor propertyDescriptor,
			PersistentEntity<?, SemanticPersistentProperty> owner,
			SimpleTypeHolder simpleTypeHolder) {
		this(field, propertyDescriptor, owner, simpleTypeHolder, null);
	}
	
	public SemanticPersistentPropertyImpl(Field field,
			PropertyDescriptor propertyDescriptor,
			PersistentEntity<?, SemanticPersistentProperty> owner,
			SimpleTypeHolder simpleTypeHolder, SemanticMappingContext mappingContext) {
		super(field, propertyDescriptor, owner, simpleTypeHolder);
		annotations = extractAnnotations(field);
		isIdProperty = annotations.containsKey(ResourceId.class);
		this.mappingContext = mappingContext;
	}

	@Override
	public String getAliasPredicate() {
		if(aliasPredicate == null){
			aliasPredicate = "urn:"+getOwner().getType().getSimpleName().toLowerCase()+":field:"+field.getName();
		}
		return aliasPredicate;
	}
	
	public String getBindingName(){
		if(bindingName == null){
			bindingName = getOwner().getType().getSimpleName().toLowerCase()+"_"+getName();
		}
		return bindingName;
	}

	private Map<Class<? extends Annotation>, ? extends Annotation> extractAnnotations(Field field) {
		Map<Class<? extends Annotation>, Annotation> extracted = new HashMap<Class<? extends Annotation>, Annotation>();
		for (Annotation annotation : field.getAnnotations()) {
			extracted.put(annotation.annotationType(), annotation);
		}
		return extracted;
	}

	@Override
	public boolean isIdProperty() {
		return isIdProperty;
	}

	@Override
	public boolean hasPredicate() {
		return annotations.containsKey(Predicate.class);
	}

	@Override
	public IRI getPredicate() {
		if (hasPredicate()) {
			String predicate = getAnnotation(Predicate.class).value();
			if(ValueUtils.isAbsoluteIRI(predicate)){
				return ValueUtils.createIRI(predicate);
			}
			return resolveWithNamespace(predicate);
		} else {
			return resolveWithNamespace(field.getName());
		}
	}

	private IRI resolveWithNamespace(String name) {
		if(this.getOwner() instanceof SemanticPersistentEntity){
			SemanticPersistentEntity<?> persistentEntity = (SemanticPersistentEntity<?>) this.getOwner();
			IRI namespace = persistentEntity.getNamespace();
			if(namespace != null){
				return ValueUtils.createIRI(namespace.stringValue(), name);
			}
		}
		return mappingContext.resolveIRI(name);
	}

	@Override
	public SemanticPersistentProperty getInverseProperty() {
		if(this.isAssociation()){
			RelatedTo relatedTo = findAnnotation(RelatedTo.class);
			String mappedProperty = relatedTo.mappedProperty();
			if(StringUtils.hasText(mappedProperty)){
				SemanticPersistentEntity<?> associatedEntity = mappingContext.getPersistentEntity(this.getActualType());
				return associatedEntity.getPersistentProperty(mappedProperty);
			}
		}
		return null;
	}

	@Override
	public boolean hasLanguage() {
		return annotations.containsKey(Language.class);
	}

	@Override
	public List<String> getLanguage() {
		final Language lang = getAnnotation(Language.class);
		if (lang != null) {
			List<String> languages = new LinkedList<String>();
			for (Languages l : lang.value()) {
				languages.add(l.toString());
			}
			return languages;
		}
		return new ArrayList<String>(0);
	}

	@SuppressWarnings("unchecked")
	private <T extends Annotation> T getAnnotation(Class<T> annotationType) {
		return (T) annotations.get(annotationType);
	}

	@Override
	public boolean hasDatatype() {
		return annotations.containsKey(Datatype.class);
	}

	@Override
	public String getDatatype() {
		final Datatype dt = getAnnotation(Datatype.class);
		if (dt != null) {
			return dt.value().toString();
		}
		return null;
	}

	@Override
	protected Association<SemanticPersistentProperty> createAssociation() {
		return new Association<SemanticPersistentProperty>(this, null);
	}

	@Override
	public Object getValue(final Object entity,
			final MappingPolicy mappingPolicy) {
		return getValueFromEntity(entity);
	}

	private Object getValueFromEntity(Object entity) {
		try {
			final Field field = getField();
			if (!field.isAccessible()) {
				field.setAccessible(true);
			}
			return field.get(entity);
		} catch (IllegalAccessException e) {
			throw new MappingException("Could not access field " + field, e);
		}
	}

	@Override
	public void setValue(Object entity, Object newValue) {
		try {
			if (!field.isAccessible()) {
				field.setAccessible(true);
			}
			field.set(entity, newValue);
		} catch (IllegalAccessException e) {
			throw new MappingException("Could not access field " + field
					+ " for setting value " + newValue + " on " + this, e);
		}
	}

	@Override
	public boolean isContext() {
		return annotations.containsKey(Context.class);
	}

	@Override
	public MappingPolicy getMappingPolicy() {
		if (annotations.containsKey(Fetch.class)){
            return new MappingPolicyImpl(Arrays.asList(((Fetch) annotations.get(Fetch.class)).value()));
		}
        else {
        	return MappingPolicyImpl.DEFAULT_POLICY;
        }
	}
	
	@Override
	public boolean shallBePersisted() {
		return super.shallBePersisted();
	}

	@Override
	public boolean isVersionProperty() {
		// TODO Auto-generated method stub
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <A extends Annotation> A findAnnotation(Class<A> annotationType) {
		return (A) annotations.get(annotationType);
	}

	@Override
	public <A extends Annotation> A findPropertyOrOwnerAnnotation(Class<A> annotationType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
		return annotations.containsKey(annotationType);
	}

	@Override
	public boolean isOptional() {
		return annotations.containsKey(Optional.class);
	}

	@Override
	public Direction getDirection() {
		if(isAssociation()){
			RelatedTo relatedTo = findAnnotation(RelatedTo.class);
			return relatedTo.direction();
		}
		else{
			return Direction.OUTGOING;
		}
	}

	@Override
	public boolean isTransient() {
		return isAnnotationPresent(Transient.class) || (field == null ? false : Modifier.isTransient(field.getModifiers()));
	}
}
