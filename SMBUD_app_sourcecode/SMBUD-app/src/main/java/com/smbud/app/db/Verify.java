package com.smbud.app.db;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import org.bson.BsonArray;
import org.bson.BsonValue;
import org.bson.Document;

import java.util.stream.Collectors;


public class Verify implements AutoCloseable {

	private static final String CONNECTION_STRING =
			"mongodb+srv://root:password1234*qwerty" +
			"@project-mongodb.2phdr.mongodb.net/" +
			"project-mongodb" +
			"?retryWrites=true&w=majority";
	private static final String DB_NAME = "project-mongodb";
	private static final String CERT_COLL = "certification";
	private static final String GOV_COLL = "government";

	private static Verify verify;

	private final MongoClient mongoClient;
	private final MongoCollection<Document> certifications;
	private final MongoCollection<Document> government;


	public static void start() {
		ConnectionString connectionString = new ConnectionString(CONNECTION_STRING);
		verify = new Verify(connectionString);
	}


	private Verify(ConnectionString connectionString) {
		MongoClientSettings mongoSettings = MongoClientSettings.builder()
				.applyConnectionString(connectionString)
				.build();
		mongoClient = MongoClients.create(mongoSettings);
		MongoDatabase mongoDatabase = mongoClient.getDatabase(DB_NAME);
		certifications = mongoDatabase.getCollection(CERT_COLL);
		government = mongoDatabase.getCollection(GOV_COLL);
	}

	public static Verify getInstance() {
		return verify;
	}


	public boolean executeVerification(String ssn) {
		int vaccineVal = 0, testVal = 0;

		try (MongoCursor<Document> cursor =
					 government.find(Document.parse(Queries.buildVaccineValidityQuery())).cursor()) {
			if(cursor.hasNext()) {
				Document gov = cursor.next();
				vaccineVal = (int) gov.get("vaccine_validity_time_month");
				testVal = (int) gov.get("test_validity_time_h");
			}
		}

		/*
		AggregateIterable<Document> t = certifications.aggregate(BsonArray.parse(Queries.buildVerificationQuery(ssn, vaccineVal, testVal)).stream().map(BsonValue::asDocument).collect(Collectors.toList()));
		System.out.println(Queries.buildVerificationQuery(ssn, vaccineVal, testVal));
		for(Document d : t) {
			System.out.println(d);
		}*/

		return certifications.aggregate(BsonArray.parse(Queries.buildVerificationQuery(ssn, vaccineVal, testVal))
				.stream()
				.map(BsonValue::asDocument)
				.collect(Collectors.toList())).iterator().hasNext();
	}

	@Override
	public void close() {
		mongoClient.close();
	}
}
