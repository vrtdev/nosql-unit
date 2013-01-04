package com.lordofthejars.nosqlunit.mongodb;

import java.io.InputStream;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lordofthejars.nosqlunit.core.AbstractCustomizableDatabaseOperation;
import com.lordofthejars.nosqlunit.core.NoSqlAssertionError;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.MongoOptions;
import com.mongodb.WriteConcern;

public final class MongoOperation extends AbstractCustomizableDatabaseOperation<MongoDbConnectionCallback, Mongo> {

	private static Logger LOGGER = LoggerFactory.getLogger(MongoOptions.class);

	private Mongo mongo;
	private MongoDbConfiguration mongoDbConfiguration;

	protected MongoOperation(Mongo mongo, MongoDbConfiguration mongoDbConfiguration) {
			this.mongo = mongo;
			this.mongoDbConfiguration = mongoDbConfiguration;
			this.setInsertionStrategy(new DefaultInsertionStrategy());
			this.setComparisonStrategy(new DefaultComparisonStrategy());
	}
	
	public MongoOperation(MongoDbConfiguration mongoDbConfiguration) {
		try {
			this.mongo = mongoDbConfiguration.getMongo();
			this.mongo.setWriteConcern(WriteConcern.SAFE);
			this.mongoDbConfiguration = mongoDbConfiguration;
			this.setInsertionStrategy(new DefaultInsertionStrategy());
			this.setComparisonStrategy(new DefaultComparisonStrategy());
		} catch (MongoException e) {
			throw new IllegalArgumentException(e);
		}
	}

	@Override
	public void insert(InputStream contentStream) {

		insertData(contentStream);

	}

	private void insertData(InputStream contentStream) {
		try {

			final DB mongoDb = getMongoDb();
			executeInsertion(new MongoDbConnectionCallback() {
				
				@Override
				public DB db() {
					return mongoDb;
				}
			}, contentStream);

		} catch (Throwable e) {
			throw new IllegalArgumentException("Unexpected error reading data set file.", e);
		}
	}


	@Override
	public void deleteAll() {
		DB mongoDb = getMongoDb();
		deleteAllElements(mongoDb);
	}

	private void deleteAllElements(DB mongoDb) {
		Set<String> collectionaNames = mongoDb.getCollectionNames();

		for (String collectionName : collectionaNames) {

			if (isNotASystemCollection(collectionName)) {

				LOGGER.debug("Dropping Collection {}.", collectionName);

				DBCollection dbCollection = mongoDb.getCollection(collectionName);
				dbCollection.drop();
			}
		}
	}

	private boolean isNotASystemCollection(String collectionName) {
		return !collectionName.startsWith("system");
	}

	@Override
	public boolean databaseIs(InputStream contentStream) {

		return compareData(contentStream);

	}

	private boolean compareData(InputStream contentStream) throws NoSqlAssertionError {
		try {
			final DB mongoDb = getMongoDb();
			executeComparison(new MongoDbConnectionCallback() {
				
				@Override
				public DB db() {
					return mongoDb;
				}
			}, contentStream);
			return true;
		} catch (NoSqlAssertionError e) {
			throw e;
		} catch (Throwable e) {
			throw new IllegalArgumentException("Unexpected error reading expected data set file.", e);
		}
	}

	private DB getMongoDb() {

		DB db = mongo.getDB(this.mongoDbConfiguration.getDatabaseName());

		if (this.mongoDbConfiguration.isAuthenticateParametersSet() && !db.isAuthenticated()) {

			boolean authenticated = db.authenticate(this.mongoDbConfiguration.getUsername(), this.mongoDbConfiguration
					.getPassword().toCharArray());

			if (!authenticated) {
				throw new IllegalArgumentException("Login/Password provided to connect to MongoDb are not valid");
			}

		}

		return db;
	}

	@Override
	public Mongo connectionManager() {
		return mongo;
	}

}
