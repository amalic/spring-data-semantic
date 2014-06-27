package org.springframework.data.semantic.support.convert;

import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.model.BeanWrapper;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.semantic.convert.state.EntityState;
import org.springframework.data.semantic.convert.state.EntityStateFactory;
import org.springframework.data.semantic.core.StatementsIterator;
import org.springframework.data.semantic.mapping.MappingPolicy;
import org.springframework.data.semantic.mapping.SemanticPersistentEntity;
import org.springframework.data.semantic.mapping.SemanticPersistentProperty;

/**
 * Class that performs transitions between entities and state (
 * {@link StatementsIterator}).
 * 
 * @author konstantin.pentchev
 * 
 */
public class SemanticSourceStateTransmitter {

	private EntityStateFactory<StatementsIterator> entityStateFactory;

	public SemanticSourceStateTransmitter(
			EntityStateFactory<StatementsIterator> entityStateFactory) {
		this.entityStateFactory = entityStateFactory;
	}

	/**
	 * 
	 * @param wrapper
	 * @param source
	 * @param persistentEntity
	 * @param mappingPolicy
	 */
	public <R> void copyPropertiesFrom(
			final BeanWrapper<SemanticPersistentEntity<R>, R> wrapper,
			StatementsIterator source,
			SemanticPersistentEntity<R> persistentEntity,
			final MappingPolicy mappingPolicy) {
		final R entity = wrapper.getBean();
		final EntityState<R, StatementsIterator> entityState = entityStateFactory.getEntityState(entity, false);
		entityState.setPersistentState(source);
		persistentEntity.doWithProperties(new PropertyHandler<SemanticPersistentProperty>() {					
			@Override
			public void doWithPersistentProperty(SemanticPersistentProperty property) {
				//Id property is populated on entity instantiation; nothing to do with it here
				if(property.isIdProperty()) {
					return;
				}

				copyEntityStatePropertyValue(property, entityState,	wrapper, mappingPolicy.combineWith(property.getMappingPolicy()));
			}
		});
		persistentEntity.doWithAssociations(new AssociationHandler<SemanticPersistentProperty>() {
			@Override
			public void doWithAssociation(Association<SemanticPersistentProperty> association) {
				final SemanticPersistentProperty property = association.getInverse();
				copyEntityStatePropertyValue(property, entityState,	wrapper, mappingPolicy.combineWith(property.getMappingPolicy()));
			}
		});
		// return entity;
	}

	private <R> Object copyEntityStatePropertyValue(SemanticPersistentProperty property, EntityState<R, StatementsIterator> state, 	BeanWrapper<SemanticPersistentEntity<R>, R> wrapper,
			final MappingPolicy mappingPolicy) {
		final Object value = state.getValue(property, mappingPolicy);
		setProperty(wrapper, property, value);
		return value;
	}

	public <R> void setProperty(BeanWrapper<SemanticPersistentEntity<R>, R> wrapper, SemanticPersistentProperty property, Object value) {
		try {
			wrapper.setProperty(property, value);
		} catch (Exception e) {
			throw new MappingException("Setting property " + property.getName()
					+ " to " + value + " on " + wrapper.getBean(), e);
		}
	}

}