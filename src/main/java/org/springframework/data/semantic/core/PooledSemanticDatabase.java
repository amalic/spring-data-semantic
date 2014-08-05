package org.springframework.data.semantic.core;

import info.aduna.iteration.Iterations;

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ContextStatementImpl;
import org.openrdf.model.impl.NamespaceImpl;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.query.BindingSet;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryInterruptedException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.repository.query.QueryCreationException;
import org.springframework.data.semantic.query.TupleSparqlQuery;
import org.springframework.data.semantic.support.database.SesameConnectionPool;

/**
 * An implementation of {@link SemanticDatabase} that uses connection pooling.
 * 
 * @author konstantin.pentchev
 *
 */
public class PooledSemanticDatabase implements SemanticDatabase{

	private SesameConnectionPool connectionPool;
	
	private Logger logger = LoggerFactory.getLogger(PooledSemanticDatabase.class);

	public PooledSemanticDatabase(Repository repository, int maxConnections){
		this(new SesameConnectionPool(repository, maxConnections, 6000));			
	}

	public PooledSemanticDatabase(SesameConnectionPool pool){
		this.connectionPool = pool;
	}

	public List<Namespace> getNamespaces() throws RepositoryException {
		RepositoryConnection con = connectionPool.getConnection();
		try {
			RepositoryResult<Namespace> repoResult = con.getNamespaces();
			return Iterations.asList(repoResult);
		} finally {
			con.close();
		}		
	}

	public void addNamespace(String prefix, String namespace)
			throws RepositoryException {
		RepositoryConnection con = connectionPool.getConnection();
		try {
			con.setNamespace(prefix, namespace);
		} finally {
			con.close();
		}		
	}

	public List<Resource> getContexts() throws RepositoryException {
		RepositoryConnection con = connectionPool.getConnection();
		try {
			RepositoryResult<Resource> contexts = con.getContextIDs();
			return Iterations.asList(contexts);
		} finally {
			con.close();
		}

	}

	public List<BindingSet> getQueryResults(String source)
			throws RepositoryException, QueryEvaluationException, MalformedQueryException {

		RepositoryConnection con = connectionPool.getConnection();		
		try{
			TupleQuery query = con.prepareTupleQuery(QueryLanguage.SPARQL, source);			
			TupleQueryResult queryResult = query.evaluate();
			List<BindingSet> result = new ArrayList<BindingSet>(); 	
			while(queryResult.hasNext()){
				result.add(queryResult.next());
			}
			return result;
		} finally {
			con.close();
		}
	}

	public List<BindingSet> getQueryResults(String source, Integer offset, Integer limit) 
			throws RepositoryException, QueryEvaluationException, MalformedQueryException {

		List<BindingSet> result = new ArrayList<BindingSet>();		
		RepositoryConnection con = connectionPool.getConnection();
		try{
			TupleSparqlQuery query = new TupleSparqlQuery(source, con);
			query.setLimit(limit.longValue());
			query.setOffset(offset.longValue());

			TupleQueryResult queryResult = query.evaluate();
			while(queryResult.hasNext()){
				result.add(queryResult.next());
			}
			return result;
		} finally {
			con.close();
		}


	}

	/**
	 * gets the size of the result which would be returned by a query upon execution
	 */
	public Long getQueryResultsCount(String source) throws RepositoryException,
	QueryEvaluationException, MalformedQueryException {

		RepositoryConnection con = connectionPool.getConnection();
		try {
			if(con.getRepository() instanceof SailRepository) {
				return getQueryResultsCountForLocalRepo(source);
			} else {
				return getQueryResultsCountForRemoteRepo(source, con);			
			}
		} finally {
			con.close();
		}

	}

	//-----------Helpers for the getQueryResultsCount method-------------------
	private Long getQueryResultsCountForLocalRepo(String source) 
			throws RepositoryException, QueryEvaluationException, MalformedQueryException {
		/* TODO dumb dumb way to check the size of the result returned by the query
		 * TODO when issued against a local repository!!!
		 * TODO check if editing the query to get COUNT(1) instead of 
		 * TODO projection vars will result in a faster query
		 * TODO and if yes, do it that way
		 * */
		List<BindingSet> result = getQueryResults(source);
		return (long) result.size();
	}

	private Long getQueryResultsCountForRemoteRepo(String source, RepositoryConnection con) 
			throws MalformedQueryException, QueryEvaluationException {
		
		TupleSparqlQuery query = new TupleSparqlQuery(source, con);
		query.setCount(true);
		TupleQueryResult queryResult = query.evaluate();

		if(!queryResult.hasNext()) {
			throw new QueryEvaluationException(String.format("Query with source %s returned no count results.", source));
		}

		List<String> bindingNames = queryResult.getBindingNames();		
		if(bindingNames.isEmpty()) {
			throw new MalformedQueryException(String.format("No binding names for query string '%s'", source));
		}
		String bindingName = bindingNames.get(0);

		String countStr = queryResult.next().getBinding(bindingName).getValue().stringValue();
		try {
			return Long.parseLong(countStr);
		} catch(NumberFormatException nfe) {
			throw new QueryEvaluationException(String.format("Evaluating count query failed for query source %s. " +
					"The returned result ('%s') for binding '%s' is not a number.", source, countStr, bindingName));
		}
	}

	//-------------------------------------------------------------------------
	
	public List<Statement> getStatementsForSubject(Resource subject){
		return getStatementsForQuadruplePattern(subject, null, null, null);
	}

	public List<Statement> getStatementsForPredicate(URI predicate){
		return getStatementsForQuadruplePattern(null, predicate, null, null);
	}

	public List<Statement> getStatementsForObject(Value object){
		return getStatementsForQuadruplePattern(null, null, object, null);
	}

	public List<Statement> getStatementsForContext(Resource context){
		return getStatementsForQuadruplePattern(null, null, null, context);
	}

	public List<Statement> getStatementsForTriplePattern(Resource subject,
			URI predicate, Value object){
		return getStatementsForQuadruplePattern(subject, predicate, object, null);
	}

	public List<Statement> getStatementsForQuadruplePattern(Resource subject,
			URI predicate, Value object, Resource context){
		RepositoryConnection con = connectionPool.getConnection();
		try {
			RepositoryResult<Statement> repoResult = con.getStatements(subject, predicate, object, true, context);
			return Iterations.asList(repoResult);
		} catch (RepositoryException e) {
			logger.error(e.getMessage(), e);
			throw new SemanticDatabaseAccessException(e);
		} finally {
			try {
				con.close();
			} catch (RepositoryException e) {
				logger.error(e.getMessage(), e);
				throw new SemanticDatabaseAccessException(e);
			}
		}		
	}

	public void addStatement(Statement statement) {
		RepositoryConnection con = connectionPool.getConnection();
		try {
			con.add(statement);
			con.commit();
		} catch (RepositoryException e) {
			logger.error(e.getMessage(),e);
			try {
				con.rollback();
			} catch (RepositoryException e1) {
				logger.error(e.getMessage(),e);
			}
			throw new SemanticDatabaseAccessException(e);
		}finally {
			try {
				con.close();
			} catch (RepositoryException e) {
				logger.error(e.getMessage(),e);
			}
		}		
	}

	public void addStatement(Resource subject, URI predicate, Value object) {
		addStatement(new StatementImpl(subject, predicate, object));
	}

	public void addStatement(Resource subject, URI predicate, Value object,
			Resource context) {
		addStatement(new ContextStatementImpl(subject, predicate, object, context));	
	}

	public void addStatements(Collection<? extends Statement> statements) {
		RepositoryConnection con = connectionPool.getConnection();
		try {
			con.add(statements);
			con.commit();
		} catch (RepositoryException e) {
			logger.error(e.getMessage(),e);
			try {
				con.rollback();
			} catch (RepositoryException e1) {
				logger.error(e.getMessage(),e);
			}
			throw new SemanticDatabaseAccessException(e);
		} finally {
			try {
				con.close();
			} catch (RepositoryException e) {
				logger.error(e.getMessage(),e);
			}
		}		
	}

	public void addStatementsFromFile(File rdfSource) {

		RDFFormat format = RDFFormat.forFileName(rdfSource.getName());
		if(format == null) {
			throw new InvalidParameterException("File should be in a valid RDF format; cannot determine one from the file extension.");
		}
		RepositoryConnection con = connectionPool.getConnection();
		try {
			con.add(rdfSource, null, format, new Resource[]{});
		} catch (RDFParseException e) {
			logger.error(e.getMessage(),e);
			try {
				con.rollback();
			} catch (RepositoryException e1) {
				logger.error(e.getMessage(),e);
			}
			throw new InvalidDataAccessApiUsageException(e.getMessage(), e);
		} catch (RepositoryException e) {
			logger.error(e.getMessage(),e);
			try {
				con.rollback();
			} catch (RepositoryException e1) {
				logger.error(e.getMessage(),e);
			}
			throw new SemanticDatabaseAccessException(e);
		} catch (IOException e) {
			logger.error(e.getMessage(),e);
			try {
				con.rollback();
			} catch (RepositoryException e1) {
				logger.error(e.getMessage(),e);
			}
			throw new UncategorizedSemanticDataAccessException(e.getMessage(), e);
		} finally {
			try {
				con.close();
			} catch (RepositoryException e) {
				logger.error(e.getMessage(),e);
			}
		}
	}

	public void removeStatement(Statement statement) {
		RepositoryConnection con = connectionPool.getConnection();
		try {
			con.remove(statement);
			con.commit();
		} catch (RepositoryException e) {
			logger.error(e.getMessage(),e);
			try {
				con.rollback();
			} catch (RepositoryException e1) {
				logger.error(e.getMessage(),e);
			}
			throw new SemanticDatabaseAccessException(e);
		}finally {
			try {
				con.close();
			} catch (RepositoryException e) {
				logger.error(e.getMessage(),e);
			}
		}		
	}

	public void removeStatement(Resource subject, URI predicate, Value object) {
		removeStatement(new StatementImpl(subject, predicate, object));	
	}

	public void removeStatement(Resource subject, URI predicate, Value object,
			Resource context) {
		removeStatement(new ContextStatementImpl(subject, predicate, object, context));	
	}
	
	@Override
	public void removeStatement(Collection<? extends Statement> statements) {
		RepositoryConnection con = connectionPool.getConnection();
		try {
			con.remove(statements);
			con.commit();
		} catch (RepositoryException e) {
			logger.error(e.getMessage(),e);
			try {
				con.rollback();
			} catch (RepositoryException e1) {
				logger.error(e.getMessage(),e);
			}
			throw new SemanticDatabaseAccessException(e);
		} finally {
			try {
				con.close();
			} catch (RepositoryException e) {
				logger.error(e.getMessage(),e);
			}
		}
	}

	@Override
	public void shutdown() {
		this.connectionPool.shutDown();
		this.connectionPool.shutdownThread();
	}

	@Override
	public Namespace getDefaultNamespace() throws RepositoryException {		
		RepositoryConnection con = connectionPool.getConnection();
		try {
			String defaultNSName = con.getNamespace("");
			if(defaultNSName == null){
				return new NamespaceImpl("", "urn:sprind-data-semantic:");
			}
			return new NamespaceImpl("", defaultNSName);
		} finally {
			con.close();
		}		
	}

	@Override
	public GraphQueryResult getStatementsForGraphQuery(String graphQuery)
			throws RepositoryException, QueryCreationException,
			QueryEvaluationException, QueryInterruptedException, MalformedQueryException {

		RepositoryConnection con = connectionPool.getConnection();		
		try{
			GraphQuery query = con.prepareGraphQuery(QueryLanguage.SPARQL, graphQuery);
			return query.evaluate();
		}
		finally {
			con.close();
		}
	}

	@Override
	public long count() {
		long size = 0;
		RepositoryConnection con = connectionPool.getConnection();
		try {
			 size = con.size();
		} catch (RepositoryException e) {
			logger.error(e.getMessage(),e);
			throw new SemanticDatabaseAccessException(e);
		} finally{
			try {
				con.close();
			} catch (RepositoryException e) {
				logger.error(e.getMessage(),e);
				throw new SemanticDatabaseAccessException(e);
			}
		}
		return size;
	}



	

}
