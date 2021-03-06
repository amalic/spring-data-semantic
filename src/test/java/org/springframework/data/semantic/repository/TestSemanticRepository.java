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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.semantic.core.SemanticDatabase;
import org.springframework.data.semantic.model.DateEntity;
import org.springframework.data.semantic.model.DateEntityRepository;
import org.springframework.data.semantic.model.ModelEntity;
import org.springframework.data.semantic.model.ModelEntityCollector;
import org.springframework.data.semantic.model.ModelEntityCollectorCascadeAll;
import org.springframework.data.semantic.model.ModelEntityCollectorCascadeAllRepository;
import org.springframework.data.semantic.model.ModelEntityCollectorRepository;
import org.springframework.data.semantic.model.ModelEntityExtended;
import org.springframework.data.semantic.model.ModelEntityExtendedRepository;
import org.springframework.data.semantic.model.ModelEntityRepository;
import org.springframework.data.semantic.model.TransientEntity;
import org.springframework.data.semantic.model.TransientEntityRepository;
import org.springframework.data.semantic.model.WineBody;
import org.springframework.data.semantic.model.WineBodyRepository;
import org.springframework.data.semantic.model.XMLGregorianCalendarEntity;
import org.springframework.data.semantic.model.XMLGregorianCalendarRepository;
import org.springframework.data.semantic.model.vocabulary.DATE_ENTITY;
import org.springframework.data.semantic.model.vocabulary.MODEL_ENTITY;
import org.springframework.data.semantic.model.vocabulary.WINE;
import org.springframework.data.semantic.model.vocabulary.XMLCALENDAR_ENTITY;
import org.springframework.data.semantic.testutils.Utils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@FixMethodOrder
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath*:META-INF/default-context.xml" })
public class TestSemanticRepository {

	@Autowired
	private WineBodyRepository wineRepository;
	
	@Autowired
	private ModelEntityRepository modelEntityRepository;
	
	@Autowired
	private ModelEntityCollectorRepository modelEntityCollectorRepository;
	
	@Autowired
	private ModelEntityCollectorCascadeAllRepository modelEntityCascadeAllRepository;

    @Autowired
    private XMLGregorianCalendarRepository gregorianCalendarRepository;

	@Autowired
	private SemanticDatabase sdb;
	
	@Autowired
	private DateEntityRepository dateEntityRepository;

	@Autowired
	private ModelEntityExtendedRepository modelEntityExtendedRepository;
	
	@Autowired
	private TransientEntityRepository transientEntityRepository;
	
	@Before
	public void initRepo() throws RepositoryException {
		// upload test data
		Utils.populateTestRepository(sdb);
		/*
		 Problem: SemanticMappingContext is now initialized lazily which postpones the
		 initialization for after the repository is populated. SemanticDatabase.getDefaultNamespace()
		 then returns <http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#> because wine.ttl says so.
		 Then tests fail because they expect the default namespace to be the hardcoded default
		 <urn:spring-data-semantic:>
		 Solution: add a default prefix to what the test expect after the repository is populated
		 */
		sdb.addNamespace("", MODEL_ENTITY.NAMESPACE);
	}
	
	@After
	public void clearRepo(){
		sdb.clear();
	}
	
	@Test
	public void testFindOne() {
		WineBody entity = wineRepository.findOne(WINE.LIGHT);
		assertNotNull(entity);
		assertEquals(WINE.LIGHT, entity.getUri());
		assertNotNull(entity.getLabel());
	}
	
	@Test
	public void testSaveUpdate(){
		long count = sdb.count();
		WineBody newEntity = new WineBody();
		newEntity.setUri(WINE.RUBIN);
		newEntity.setLabel("Rubin");
		wineRepository.save(newEntity);
		assertTrue(count < sdb.count());
		List<Statement> statementsForResource = sdb.getStatementsForSubject(WINE.RUBIN);
		assertFalse(statementsForResource.isEmpty());
		//System.out.println(statementsForResource);
		WineBody rubin = wineRepository.findOne(WINE.RUBIN);
		assertNotNull(rubin);
	}
	
	@Test
	public void testCreate(){
		long count = sdb.count();
		WineBody newEntity = new WineBody();
		newEntity.setUri(WINE.VERDEJO);
		newEntity.setLabel("Verdejo");
		wineRepository.create(newEntity);
		assertTrue(count < sdb.count());
		List<Statement> statementsForResource = sdb.getStatementsForSubject(WINE.VERDEJO);
		assertFalse(statementsForResource.isEmpty());
		//System.out.println(statementsForResource);
		WineBody verdejo = wineRepository.findOne(WINE.VERDEJO);
		assertNotNull(verdejo);
	}
	
	@Test
	public void testSaveMultiple(){
		assertNull(wineRepository.findOne(WINE.GAMZA));
		assertNull(wineRepository.findOne(WINE.KADARKA));
		
		WineBody gamza = new WineBody();
		gamza.setLabel("Gamza");
		gamza.setUri(WINE.GAMZA);
		
		WineBody kadarka = new WineBody();
		kadarka.setLabel("Kadarka");
		kadarka.setUri(WINE.KADARKA);
		
		Iterable<WineBody> newWineBodies = wineRepository.save(Arrays.asList(gamza, kadarka));
		assertNotNull(newWineBodies);
		assertTrue(newWineBodies.iterator().hasNext());
		
		assertNotNull(wineRepository.findOne(WINE.GAMZA));
		assertNotNull(wineRepository.findOne(WINE.KADARKA));
	}
	
	@Test
	public void testCreateMultiple(){
		assertNull(wineRepository.findOne(WINE.MACABEO));
		assertNull(wineRepository.findOne(WINE.SAUVIGNON_BLANC));
		
		WineBody macabeo = new WineBody();
		macabeo.setLabel("Macabeo");
		macabeo.setUri(WINE.MACABEO);
		
		WineBody sauvignon_blanc = new WineBody();
		sauvignon_blanc.setLabel("Sauvignon blanc");
		sauvignon_blanc.setUri(WINE.SAUVIGNON_BLANC);
		
		Iterable<WineBody> newWineBodies = wineRepository.create(Arrays.asList(macabeo, sauvignon_blanc));
		assertNotNull(newWineBodies);
		assertTrue(newWineBodies.iterator().hasNext());
		
		assertNotNull(wineRepository.findOne(WINE.MACABEO));
		assertNotNull(wineRepository.findOne(WINE.SAUVIGNON_BLANC));
	}
	
	@Test
	public void testCollectionOfAssociations(){
		ModelEntity modelEntity = modelEntityRepository.findOne(MODEL_ENTITY.ENTITY_ONE);
		assertNotNull(modelEntity);
		assertNotNull(modelEntity.getRelated());
		assertEquals(2, modelEntity.getRelated().size());
		
	}
	
	@Test
	public void testModificationOfCollectionOfAssociations(){
		long count = sdb.count();
		ModelEntity modelEntity = modelEntityRepository.findOne(MODEL_ENTITY.ENTITY_TWO);
		modelEntity.getRelated().remove(1);
		modelEntityRepository.save(modelEntity);
		assertEquals(count -2, sdb.count());
	}
	
	@Test
	public void testCount(){
		assertEquals(4, modelEntityRepository.count());
	}
	
	@Test
	public void testFindList(){
		List<IRI> uris = Arrays.asList(MODEL_ENTITY.ENTITY_ONE, MODEL_ENTITY.ENTITY_TWO);
		Iterable<ModelEntity> entities = modelEntityRepository.findAll(uris);
		int count = 0;
		for(ModelEntity entity : entities){
			assertTrue(uris.contains(entity.getUri()));
			count++;
		}
		assertEquals(uris.size(), count);
	}
	
	@Test
	public void testExists(){
		assertTrue(modelEntityRepository.exists(MODEL_ENTITY.ENTITY_ONE));
		assertFalse(modelEntityRepository.exists(MODEL_ENTITY.ENTITY_NOT_EXISTS));
	}
	
	@Test
	public void testFindAll(){
		Iterable<ModelEntity> modelEntities = modelEntityRepository.findAll();
		int count = 0;
		for(ModelEntity entity : modelEntities){
			assertNotNull(entity.getUri());
			count++;
		}
		assertEquals(modelEntityRepository.count(), count);
	}
	
	@Test
	public void testFindPage(){
		int pageSize = 2;
		Iterable<ModelEntity> modelEntities = modelEntityRepository.findAll(new PageRequest(0, pageSize));
		int count = 0;
		for(ModelEntity entity : modelEntities){
			assertNotNull(entity.getUri());
			count++;
		}
		assertEquals(pageSize, count);
	}
	
	@Test
	public void testEagerLoad(){
		ModelEntityCollector collector = modelEntityCollectorRepository.findOne(MODEL_ENTITY.COLLECTOR_ONE);
		assertNotNull(collector);
		assertFalse(collector.getEntities().isEmpty());
		for(ModelEntity modelEntity : collector.getEntities()){
			assertNotNull(modelEntity.getName());
			assertFalse(modelEntity.getRelated().isEmpty());
		}
	}
	
	@Test
	public void testEagerSaveFail(){
		ModelEntityCollector collector = new ModelEntityCollector();
		collector.setUri(MODEL_ENTITY.COLLECTOR_TWO);
		ModelEntity entity = new ModelEntity();
		entity.setName("new entity");
		entity.setUri(MODEL_ENTITY.ENTITY_NOT_EXISTS_TWO);
		collector.setEntities(Arrays.asList(entity));
		assertNotNull(modelEntityCollectorRepository.save(collector));
		assertNull(modelEntityRepository.findOne(MODEL_ENTITY.ENTITY_NOT_EXISTS_TWO));
	}
	
	@Test
	public void testEagerSaveSuccess(){
		ModelEntityCollectorCascadeAll collector = new ModelEntityCollectorCascadeAll();
		collector.setUri(MODEL_ENTITY.COLLECTOR_TWO);
		ModelEntity entity = new ModelEntity();
		entity.setName("new entity");
		entity.setUri(MODEL_ENTITY.ENTITY_NOT_EXISTS_TWO);
		collector.setEntities(Arrays.asList(entity));
		assertNotNull(modelEntityCascadeAllRepository.save(collector));
		assertNotNull(modelEntityRepository.findOne(MODEL_ENTITY.ENTITY_NOT_EXISTS_TWO));
	}
	
	@Test
	public void testDateLoad(){
		DateEntity date = dateEntityRepository.findOne(DATE_ENTITY.DATE_ONE);
		assertNotNull(date);
		assertNotNull(date.getDate());
	}
	
	@Test
	public void testDateSave(){
		Date date = new Date();
		DateEntity dateEntity = new DateEntity();
		dateEntity.setId(DATE_ENTITY.DATE_TWO);
		dateEntity.setDate(date);
		assertNotNull(dateEntityRepository.save(dateEntity));
		
		dateEntity = dateEntityRepository.findOne(DATE_ENTITY.DATE_TWO);
		assertNotNull(dateEntity);
		assertEquals(date, dateEntity.getDate());
	}

    @Test
    public void testXmlDateLoad(){
        XMLGregorianCalendarEntity date = gregorianCalendarRepository.findOne(XMLCALENDAR_ENTITY.DATE_ONE);
        assertNotNull(date);
        assertNotNull(date.getDate());
    }

    @Test
    public void testXmlCalendarDateSave(){
        String xmlDate = "1922-12-14";
        String xmlYear = "-0429";
        String xmlYearMonth = "1947-10";

        XMLGregorianCalendar xmlCalDate = XMLDatatypeUtil.parseCalendar(xmlDate);
        XMLGregorianCalendar xmlCalYear = XMLDatatypeUtil.parseCalendar(xmlYear);
        XMLGregorianCalendar xmlCalYearMonth = XMLDatatypeUtil.parseCalendar(xmlYearMonth);

        assertNotNull(xmlCalDate);
        assertNotNull(xmlCalYear);
        assertNotNull(xmlCalYearMonth);

        XMLGregorianCalendarEntity xmlGregorianCalendarEntity1 = new XMLGregorianCalendarEntity();
        xmlGregorianCalendarEntity1.setId(XMLCALENDAR_ENTITY.DATE_ONE);
        xmlGregorianCalendarEntity1.setDate(xmlCalDate);

        XMLGregorianCalendarEntity xmlGregorianCalendarEntity2 = new XMLGregorianCalendarEntity();
        xmlGregorianCalendarEntity2.setId(XMLCALENDAR_ENTITY.YEAR_ONE);
        xmlGregorianCalendarEntity2.setDate(xmlCalYear);

        XMLGregorianCalendarEntity xmlGregorianCalendarEntity3 = new XMLGregorianCalendarEntity();
        xmlGregorianCalendarEntity3.setId(XMLCALENDAR_ENTITY.YEAR_MONTH_ONE);
        xmlGregorianCalendarEntity3.setDate(xmlCalYearMonth);

        assertNotNull(gregorianCalendarRepository.save(xmlGregorianCalendarEntity1));
        assertNotNull(gregorianCalendarRepository.save(xmlGregorianCalendarEntity2));
        assertNotNull(gregorianCalendarRepository.save(xmlGregorianCalendarEntity3));

        xmlGregorianCalendarEntity1 = gregorianCalendarRepository.findOne(XMLCALENDAR_ENTITY.DATE_ONE);
        assertNotNull(xmlGregorianCalendarEntity1);
        assertEquals(xmlCalDate, xmlGregorianCalendarEntity1.getDate());

        xmlGregorianCalendarEntity2 = gregorianCalendarRepository.findOne(XMLCALENDAR_ENTITY.YEAR_ONE);
        assertNotNull(xmlGregorianCalendarEntity2);
        assertEquals(xmlCalYear, xmlGregorianCalendarEntity2.getDate());

        xmlGregorianCalendarEntity3 = gregorianCalendarRepository.findOne(XMLCALENDAR_ENTITY.YEAR_MONTH_ONE);
        assertNotNull(xmlGregorianCalendarEntity3);
        assertEquals(xmlCalYearMonth, xmlGregorianCalendarEntity3.getDate());

    }



    @Test
	public void testDelete(){
		ModelEntity toDelete = new ModelEntity();
		toDelete.setUri(MODEL_ENTITY.ENTITY_FIVE);
		toDelete.setName("Model Entity Five");
		
		modelEntityRepository.save(toDelete);
		assertTrue(modelEntityRepository.exists(MODEL_ENTITY.ENTITY_FIVE));
		
		modelEntityRepository.delete(MODEL_ENTITY.ENTITY_FIVE);
		
		assertFalse(modelEntityRepository.exists(MODEL_ENTITY.ENTITY_FIVE));
	}
	
	@Test
	public void testDeleteEntity(){
		ModelEntity toDelete = new ModelEntity();
		toDelete.setUri(MODEL_ENTITY.ENTITY_FIVE);
		toDelete.setName("Model Entity Five");
		
		toDelete = modelEntityRepository.save(toDelete);
		assertTrue(modelEntityRepository.exists(MODEL_ENTITY.ENTITY_FIVE));
		
		modelEntityRepository.delete(toDelete);
		
		assertFalse(modelEntityRepository.exists(MODEL_ENTITY.ENTITY_FIVE));
	}
	
	@Test
	public void testFindBySimpleProperty(){
		List<ModelEntity> entities = modelEntityRepository.findByName("Model Entity One");
		assertNotNull(entities);
		assertFalse(entities.isEmpty());
		assertEquals(MODEL_ENTITY.ENTITY_ONE, entities.get(0).getUri());
	}
	
	@Test
	public void testFindByCollectionProperty(){
		List<ModelEntity> entities = modelEntityRepository.findBySynonyms(Arrays.asList("Model Entity Eins", "Model Entity Uno"));
		assertNotNull(entities);
		assertFalse(entities.isEmpty());
		assertEquals(MODEL_ENTITY.ENTITY_ONE, entities.get(0).getUri());
	}
	
	@Test
	public void testFindByAssociation(){
		ModelEntity modelEntityThree = new ModelEntity();
		modelEntityThree.setUri(MODEL_ENTITY.ENTITY_THREE);
		List<ModelEntity> entities = modelEntityRepository.findByRelated(Arrays.asList(modelEntityThree));
		assertNotNull(entities);
		assertEquals(2, entities.size());
		for(ModelEntity entity : entities){
			assertTrue(entity.getUri().equals(MODEL_ENTITY.ENTITY_ONE) || entity.getUri().equals(MODEL_ENTITY.ENTITY_TWO));
		}
	}
	
	@Test
	public void testFindByAssociationIRI(){
		List<ModelEntity> entities = modelEntityRepository.findByRelated(MODEL_ENTITY.ENTITY_THREE);
		assertNotNull(entities);
		assertEquals(2, entities.size());
		for(ModelEntity entity : entities){
			assertTrue(entity.getUri().equals(MODEL_ENTITY.ENTITY_ONE) || entity.getUri().equals(MODEL_ENTITY.ENTITY_TWO));
		}
	}
	
	@Test
	public void testOneFindByAssociationIRI(){
		ModelEntity entity = modelEntityRepository.findOneByRelated(MODEL_ENTITY.ENTITY_THREE);
		assertNotNull(entity);
		assertTrue(entity.getUri().equals(MODEL_ENTITY.ENTITY_ONE) || entity.getUri().equals(MODEL_ENTITY.ENTITY_TWO));
	}
	
	@Test
	public void testCountBySimpleProperty(){
		long count = modelEntityRepository.countByName("Model Entity One");
		assertEquals(1, count);
	}
	
	@Test
	public void testCountByCollectionProperty(){
		long count = modelEntityRepository.countBySynonyms(Arrays.asList("Model Entity Eins", "Model Entity Uno"));
		assertEquals(1, count);
	}
	
	@Test
	public void testCountByAssociation(){
		ModelEntity modelEntityThree = new ModelEntity();
		modelEntityThree.setUri(MODEL_ENTITY.ENTITY_THREE);
		long count = modelEntityRepository.countByRelated(Arrays.asList(modelEntityThree));
		assertEquals(2, count);
	}
	
	@Test
	public void testCountByAssociationIRI(){
		long count = modelEntityRepository.countByRelated(MODEL_ENTITY.ENTITY_THREE);
		assertEquals(2, count);
	}

	@Test
	public void testDeleteAll(){
		modelEntityRepository.deleteAll();
		assertEquals(0, modelEntityRepository.count());
	}
	
	@Test
	public void testAddExtendedEntity(){
		ModelEntityExtended mee = new ModelEntityExtended();
		mee.setUri(MODEL_ENTITY.ENTITY_EXTENDED);
		mee.setName("Model Entity Extended One");
		mee.setSynonyms(Arrays.asList("Model Entity Erweitert Eins"));
		mee.setDummyProperty("blq");
		mee = modelEntityExtendedRepository.save(mee);
		assertNotNull(mee);
		
		mee = modelEntityExtendedRepository.findOne(MODEL_ENTITY.ENTITY_EXTENDED);
		assertNotNull(mee);
		assertNotNull(mee.getUri());
		assertNotNull(mee.getName());
		assertNotNull(mee.getSynonyms());
		
		ModelEntity me = modelEntityRepository.findOne(MODEL_ENTITY.ENTITY_EXTENDED);
		assertNotNull(me);
	}
	
	@Test
	public void testTransientEntity(){
		TransientEntity entity = new TransientEntity();
		entity.setUri(MODEL_ENTITY.ENTITY_TRANSIENT);
		entity.setTestProperty(new SimpleDateFormat("YYYY"));
		
		entity = transientEntityRepository.save(entity);
		assertNotNull(entity);
		TransientEntity search = transientEntityRepository.findOne(MODEL_ENTITY.ENTITY_TRANSIENT);
		assertNotNull(search);
		assertNull(search.getTestProperty());
	}
}
