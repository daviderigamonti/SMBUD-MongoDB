package com;


import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Sorts;
import org.bson.Document;

import static com.mongodb.client.model.Filters.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Base64;

public final class MyDb {
    private static Logger mongoLogger;
    private static ConnectionString connectionString;
    private static MongoClient mongoClient;
    private static MongoDatabase database;
    private static MongoClientSettings settings;

    private static MyDb istance;

    private static final String connectionFile = "ConnectionString.txt";


    private MyDb(){
        mongoLogger = Logger.getLogger( "org.mongodb.driver" );
        mongoLogger.setLevel(Level.SEVERE);

        try {
            String loadedConnectionString = loadConnectionString();
            if(loadedConnectionString.isEmpty())
                throw new IOException();
            connectionString = new ConnectionString(loadedConnectionString);
            settings = MongoClientSettings.builder()
                    .applyConnectionString(connectionString)
                    .build();
            mongoClient = MongoClients.create(settings);
            database = mongoClient.getDatabase("project2");
            if(!mongoClient.listDatabaseNames().cursor().hasNext())    // Check connection
                throw new Exception();
        } catch(IOException e) {
            System.out.println("Cannot access connection file");
        } catch(Exception e) {
            System.out.println("Cannot establish a connection to the MongoDB database");
        }

    }

    private String loadConnectionString() throws IOException {
        return Files.lines(Paths.get(connectionFile)).findFirst().orElse("");
    }

    public static MyDb getIstance(){
        if (istance == null)
            istance = new MyDb();
        return istance;
    }

    public static String checkSsn(String ssn, String type){
        MongoCollection<Document> collection = database.getCollection("certificate");
        Bson filter = null;
        Document myDoc = null;
        switch(type) {
            case "1 Dose":
                filter = and(eq("person.ssn", ssn),eq("vaccine.doses", 1));
                myDoc = collection.find(filter).first();
                break;
            case "2 Dose":
                filter = and(eq("person.ssn", ssn),eq("vaccine.doses", 2));
                myDoc = collection.find(filter).first();
                break;
            case "3 Dose":
                filter = and(eq("person.ssn", ssn),eq("vaccine.doses", 3));
                myDoc = collection.find(filter).first();
                break;
            case "Negative Test":
                filter = and(eq("person.ssn", ssn),eq("test.result", "Negative"));
                myDoc = collection.find(filter).sort(Sorts.descending("test.date")).first();
                break;
            case "Healed Test":
                filter = and(eq("person.ssn", ssn), eq("test.result", "Healed"));
                myDoc = collection.find(filter).sort(Sorts.descending("test.date")).first();
                break;
            default:
                // code block
        }
        if( myDoc == null || myDoc.isEmpty()){
            return null;
        }
        else {
            System.out.println(myDoc.get("_id"));
            byte[] encodedBytes = Base64.getEncoder().encode(myDoc.get("_id").toString().getBytes());
            System.out.println("encodedBytes " + new String(encodedBytes));
            return new String(encodedBytes);
        }
    }

    public static String checkCertificate(String id){
        String output = null;
        MongoCollection<Document> collection = database.getCollection("certificate");
        Document certificate = collection.find(eq("_id", new ObjectId(id))).first();
        if(certificate == null){
            output = "0" + ";" +"The certificate is not valid!";
            return output;
        }
        else{
            Document myDoc = database.getCollection("government_doc").find().first();
            Integer healed_test_month = Integer.parseInt(myDoc.get("healed_test_validity_time_month").toString());
            Integer negative_test_hour = Integer.parseInt(myDoc.get("negative_test_validity_time_h").toString());
            Integer vaccine_month = Integer.parseInt(myDoc.get("vaccine_validity_time_month").toString());
            if(certificate.containsKey("vaccine")){
                Document person = (Document) certificate.get("person");
                Document vaccine = (Document) certificate.get("vaccine");
                String ssn = person.get("ssn").toString();
                Integer dose = Integer.parseInt(vaccine.get("doses").toString());
                Bson filter = and(eq("person.ssn", ssn),gt("vaccine.doses", dose));
                Document doc = collection.find(filter).first();
                if( doc == null || doc.isEmpty()){
                    LocalDateTime now = LocalDateTime.now().minusMonths(vaccine_month);
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                    String vaccine_date_string = vaccine.get("date").toString() + " 00:00";
                    LocalDateTime vaccine_date = LocalDateTime.parse(vaccine_date_string,dtf);
                    System.out.println(vaccine_date.toString()+ "____" + dtf.format(now));
                    if(vaccine_date.isEqual(now) || vaccine_date.isAfter(now)){
                        output = "2" + ";" + person.get("name").toString() + ";" + person.get("surname").toString() +";"
                                + person.get("birthdate").toString();
                        return output;
                    }
                }
                output = "1" + ";" +"The certificate is expired";
                return output;
            }
            else
            {
                Document person = (Document) certificate.get("person");
                Document test = (Document) certificate.get("test");
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                String test_date_string = test.get("date").toString() + " 00:00";
                LocalDateTime test_date = LocalDateTime.parse(test_date_string,dtf);
                LocalDateTime now = null;

                if(test.get("result").toString().equals("Negative"))
                    now = LocalDateTime.now().minusHours(negative_test_hour);
                else
                    now = LocalDateTime.now().minusMonths(healed_test_month);
                System.out.println(test_date.toString()+ "____" + dtf.format(now));
                if(test_date.isEqual(now) || test_date.isAfter(now)){
                    output = "2" + ";" + person.get("name").toString() + ";" + person.get("surname").toString() + ";"
                            + person.get("birthdate").toString();
                    return output;
                }
                else
                {
                    output = "1" + ";" +"The certificate is expired";
                    return output;
                }
            }
        }
    }
}
