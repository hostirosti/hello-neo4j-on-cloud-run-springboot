package com.google.neo4jtogo.demo;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.TransactionWork;
import org.neo4j.driver.exceptions.Neo4jException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class DemoApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoApplication.class.getName());

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}
    private final Driver driver;
    private final ConfigurableApplicationContext applicationContext;

    // Autowire the Driver bean by constructor injection
    public DemoApplication(Driver driver, ConfigurableApplicationContext applicationContext) {
        this.driver = driver;
        this.applicationContext = applicationContext;
    }

    private static TransactionWork<Record> createRelationshipToPeople(String person1, String person2) {
        return tx -> {
            // To learn more about the Cypher syntax, see https://neo4j.com/docs/cypher-manual/current/
            // The Reference Card is also a good resource for keywords https://neo4j.com/docs/cypher-refcard/current/

            String createRelationshipToPeopleQuery = "MERGE (p1:Person { name: $person1_name }) \n" +
                    "MERGE (p2:Person { name: $person2_name })\n" +
                    "MERGE (p1)-[:KNOWS]->(p2)\n" +
                    "RETURN p1, p2";

            Map<String, Object> params = new HashMap<>();
            params.put("person1_name", person1);
            params.put("person2_name", person2);

            try {
                Result result = tx.run(createRelationshipToPeopleQuery, params);
                // You should not return the result itself outside of the scope of the transaction.
                // The result will be closed when the transaction closes and it won't be usable afterwards.
                // As we know that the query can only return one row, we can use the single method of the Result and
                // return the record.
                return result.single();

                // You should capture any errors along with the query and data for traceability
            } catch (Neo4jException ex) {
                LOGGER.error(createRelationshipToPeopleQuery + " raised an exception", ex);
                throw ex;
            }
        };
    }

    private static TransactionWork<List<String>> readPersonByName(String name) {
        return tx -> {
            String readPersonByNameQuery = "MATCH (p:Person)\n" +
                    "    WHERE p.name = $person_name\n" +
                    "    RETURN p.name AS name";

            Map<String, Object> params = Collections.singletonMap("person_name", name);

            try {
                Result result = tx.run(readPersonByNameQuery, params);
                return result.list(row -> row.get("name").asString());
            } catch (Neo4jException ex) {
                LOGGER.error(readPersonByNameQuery + " raised an exception", ex);
                throw ex;
            }
        };
    }

    @RequestMapping("/")
    public String index() {
        try (Session session = driver.session()) {
            // Using transaction functions allows the driver to handle retries and transient errors for you

            // The first examples indicates a write transaction that must go through the leader of a cluster
            Record peopleCreated = session.writeTransaction(createRelationshipToPeople("Alice", "David"));
            LOGGER.info("Create successful: " + peopleCreated.get("p1") + ", " + peopleCreated.get("p2"));

            final StringBuilder names = new StringBuilder();

            // The second examples indicates a read transaction, that can be answered by a follower
            session
                    .readTransaction(readPersonByName("Alice"))
                    .forEach((val)->{
                        names.append(val + ", ");
            });

            return  names.toString();
        } catch (Exception e) {
            return String.format("Something went wrong -- %s", e.getMessage());
        }
    }
}

