package com.dynacore.livemap.traveltime.repo;

import com.dynacore.livemap.testing.database.AbstractDatabaseClientIntegrationTests;
import com.dynacore.livemap.testing.database.ExternalDatabase;
import com.dynacore.livemap.testing.database.PostgresTestSupport;
import io.r2dbc.spi.ConnectionFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Hooks;
import reactor.test.StepVerifier;

import javax.sql.DataSource;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.Assert.*;


public class TravelTimeRepoTest extends AbstractDatabaseClientIntegrationTests {

    private DatabaseClient client = DatabaseClient.create(createConnectionFactory());
    private TravelTimeRepo repo = new TravelTimeRepo(client);

    TravelTimeEntity entityOne, entitySameAsOne, entitySameAsOneWithNewPubDate, entitySameAsOneChangedProperties, entityTwo;

    public static final ExternalDatabase database = PostgresTestSupport.database();

    @Override
    protected DataSource createDataSource() {
        return PostgresTestSupport.createDataSource(database);
    }

    @Override
    protected ConnectionFactory createConnectionFactory() {
        return PostgresTestSupport.createConnectionFactory(database);
    }

    @Before
    public void setup() {

        String pubDate = "2019-10-16T15:52:00Z";
        String retDate = "2019-10-16T16:00:00Z";

        entityOne = new TravelTimeEntity(null, "002", "First entity", OffsetDateTime.parse(pubDate), OffsetDateTime.parse(retDate), "type", 200, 5, 100);
        entitySameAsOne = new TravelTimeEntity(null, "002", "First entity", OffsetDateTime.parse(pubDate), OffsetDateTime.parse(retDate), "type", 200, 5, 100);

        String newPubDate = "2019-10-16T15:53:00Z";
        entitySameAsOneWithNewPubDate = new TravelTimeEntity(null, "002", "First entity", OffsetDateTime.parse(newPubDate), OffsetDateTime.parse(retDate), "type", 200, 5, 100);

        int newLength = 100;
        int newTravelTime = 90;
        int newVelocity = 60;

        entitySameAsOneChangedProperties = new TravelTimeEntity(null, "002", "First entity", OffsetDateTime.parse(newPubDate), OffsetDateTime.parse(retDate), "type", newLength, newTravelTime, newVelocity);
        entityTwo = new TravelTimeEntity(null, "009", "Second Entity", OffsetDateTime.parse(pubDate), OffsetDateTime.parse(retDate), "type", 200, 5, 100);

        System.out.println("Database configuration-------------");
        System.out.println("name: " + database.getDatabase());
        System.out.println("host: " + database.getHostname());
        System.out.println("jdbc url: " + database.getJdbcUrl());
        System.out.println("port: " + database.getPort());
        System.out.println("user name: " + database.getUsername());
        System.out.println("-----------------------------------");
    }

    public void dropCreate(DatabaseClient client) {

        List<String> statements = Arrays.asList(//
                "DROP TABLE IF EXISTS travel_time_entity;",
                "CREATE TABLE TRAVEL_TIME_ENTITY\n" +
                        "(\n" +
                        "    pkey SERIAL PRIMARY KEY,\n" +
                        "    id                         VARCHAR(200),\n" +
                        "    name                       VARCHAR(200),\n" +
                        "    pub_date                   TIMESTAMP WITH TIME ZONE  NOT NULL,\n" +
                        "    retrieved_from_third_party TIMESTAMP WITH TIME ZONE  NOT NULL,\n" +
                        "    type                       VARCHAR(50),\n" +
                        "    length                     SMALLINT CHECK (length >= -1),\n" +
                        "    velocity                   SMALLINT CHECK (velocity >= -1),\n" +
                        "    travel_time                SMALLINT CHECK (travel_time >= -1),\n" +
                        "    unique (id, pub_date)\n" +
                        ");");

        statements.forEach(it -> client.execute(it) //
                .fetch() //
                .rowsUpdated() //
                .as(StepVerifier::create) //
                .expectNext(0)
                .verifyComplete());
    }

    @Test
    public void insertEntityOne() {
        dropCreate(client);

        String pubDate = "2019-10-16T15:52:00Z";
        String retDate = "2019-10-16T16:00:00Z";
        TravelTimeEntity  entityOne = new TravelTimeEntity(null, "002", "First entity", OffsetDateTime.parse(pubDate), OffsetDateTime.parse(retDate), "type", 200, 5, 100);

        client.insert()
                .into(TravelTimeEntity.class)
                .using(entityOne)
                .fetch()
                .rowsUpdated()
                .as(StepVerifier::create)
                .expectNext(1)
                .verifyComplete();
    }

    @Test
    public void insertOneEntity() throws InterruptedException {

        dropCreate(client);
        String pubDate = "2019-10-16T15:52:00Z";
        String retDate = "2019-10-16T16:00:00Z";
        TravelTimeEntity  entityOne = new TravelTimeEntity(null, "002", "First entity", OffsetDateTime.parse(pubDate), OffsetDateTime.parse(retDate), "type", 200, 5, 100);
        client.insert()
                .into(TravelTimeEntity.class)
                .using(entityOne)
                .fetch()
                .rowsUpdated()
                .subscribe((x)-> System.out.println("Result consumed:" + x));

        Thread.sleep(1000);

    }

    @Test
    public void isNew() {

        dropCreate(client);

        String pubDate = "2019-10-16T15:52:00Z";
        String retDate = "2019-10-16T16:00:00Z";

        entityOne = new TravelTimeEntity(null, "002", "First entity", OffsetDateTime.parse(pubDate), OffsetDateTime.parse(retDate), "type", 200, 5, 100);
        entitySameAsOne = new TravelTimeEntity(null, "002", "First entity", OffsetDateTime.parse(pubDate), OffsetDateTime.parse(retDate), "type", 200, 5, 100);

        client.insert()
                .into(TravelTimeEntity.class)
                .using(entityOne)
                .fetch()
                .rowsUpdated()
                .as(StepVerifier::create)
                .expectNext(1)
                .verifyComplete();

        Boolean isNew = Optional.ofNullable(repo.isNew(entitySameAsOne).block())
                .orElseThrow(NoSuchElementException::new);
        assertFalse(isNew);

        String newPubDate = "2019-10-16T15:53:00Z";
        entitySameAsOneWithNewPubDate = new TravelTimeEntity(null, "002", "First entity", OffsetDateTime.parse(newPubDate), OffsetDateTime.parse(retDate), "type", 200, 5, 100);

        isNew = Optional.ofNullable(repo.isNew(entitySameAsOneWithNewPubDate).block())
                .orElseThrow(NoSuchElementException::new);

        assertTrue(isNew);
    }

    @Test
    public void saveOneThenIgnoreSame() {

        dropCreate(client);
        repo.save(entityOne).block();

        client.select()
                .from(TravelTimeEntity.class)
                .fetch()
                .first()
                .as(StepVerifier::create)
                .consumeNextWith(retrieved-> {
                    assertSame(retrieved.getPkey(), 1);
                    assertEquals(retrieved.getId(), "002");
                    assertEquals(retrieved.getName(), "First entity");
                    assertTrue(retrieved.getPubDate().isEqual(OffsetDateTime.parse("2019-10-16T15:52:00Z")));
                    assertTrue(retrieved.getRetrievedFromThirdParty().isEqual(OffsetDateTime.parse("2019-10-16T16:00:00Z")));
                    assertEquals(retrieved.getType(), "type");
                    assertEquals(200, retrieved.getLength());
                    assertEquals(5, retrieved.getTravel_time());
                    assertEquals(100, retrieved.getVelocity());
                })
                .verifyComplete();

        repo.save(entityOne).block();

        client.select()
              .from(TravelTimeEntity.class)
              .fetch()
              .all()
              .as(StepVerifier::create)
              .expectNextCount(1)
              .verifyComplete();
    }

    @Test
    public void getLastStored() {
        dropCreate(client);
        insertEntityOne();
        repo.getLatest(entityOne)
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    public void getHistoryMetaData() {
        Hooks.onOperatorDebug();
        dropCreate(client);

        String retDate = "2019-10-16T16:00:00Z";
        String pubDate = "2020-10-16T15:52:00Z";
        String pubDate2 = "2020-10-17T15:52:00Z";
        String pubDate3 = "2020-10-18T15:52:00Z";
        String pubDate4 = "2020-10-19T15:52:00Z";

        TravelTimeEntity  entityOne = new TravelTimeEntity(null, "002", "First entity", OffsetDateTime.parse(pubDate), OffsetDateTime.parse(retDate), "type", 200, 5, 100);
        TravelTimeEntity  x = new TravelTimeEntity(null, "002", "First entity", OffsetDateTime.parse(pubDate2), OffsetDateTime.parse(retDate), "type", 200, 5, 100);
        TravelTimeEntity  y = new TravelTimeEntity(null, "002", "First entity", OffsetDateTime.parse(pubDate3), OffsetDateTime.parse(retDate), "type", 200, 5, 100);
        TravelTimeEntity  z = new TravelTimeEntity(null, "002", "First entity", OffsetDateTime.parse(pubDate4), OffsetDateTime.parse(retDate), "type", 200, 5, 100);

        repo.save(entityOne).block();
        repo.save(x).block();
        repo.save(y).block();
        repo.save(z).block();

        repo.getReplayMetaData()
                .as(StepVerifier::create)
                .expectNextCount(4)
                .verifyComplete();
    }

    @Test
    public void getHistoryMetaDataRange() {
        Hooks.onOperatorDebug();
        dropCreate(client);

        String retDate = "2019-10-16T16:00:00Z";


        String pubDate = "2020-10-16T15:52:00Z";
        String pubDate2 = "2020-10-17T15:52:00Z";
        String pubDate3 = "2020-10-18T15:52:00Z";
        String pubDate4 = "2020-10-19T15:52:00Z";

        TravelTimeEntity  entityOne = new TravelTimeEntity(null, "002", "First entity", OffsetDateTime.parse(pubDate), OffsetDateTime.parse(retDate), "type", 200, 5, 100);
        TravelTimeEntity  x = new TravelTimeEntity(null, "002", "First entity", OffsetDateTime.parse(pubDate2), OffsetDateTime.parse(retDate), "type", 200, 5, 100);
        TravelTimeEntity  y = new TravelTimeEntity(null, "002", "First entity", OffsetDateTime.parse(pubDate3), OffsetDateTime.parse(retDate), "type", 200, 5, 100);
        TravelTimeEntity  z = new TravelTimeEntity(null, "002", "First entity", OffsetDateTime.parse(pubDate4), OffsetDateTime.parse(retDate), "type", 200, 5, 100);

        repo.save(entityOne).block();
        repo.save(x).block();
        repo.save(y).block();
        repo.save(z).block();

        repo.getReplayMetaData(OffsetDateTime.parse(pubDate2), OffsetDateTime.parse(pubDate4))
                .as(StepVerifier::create)
                .expectNextCount(3)
                .verifyComplete();
    }

    @Test
    public void getFeatureDateRange() {
        Hooks.onOperatorDebug();
        dropCreate(client);

        String retDate = "2019-10-16T16:00:00Z";
        String pubDate = "2020-10-16T15:52:00Z";
        String pubDate2 = "2020-10-17T15:52:00Z";
        String pubDate3 = "2020-10-18T15:52:00Z";
        String pubDate4 = "2020-10-19T15:52:00Z";

        TravelTimeEntity  entityOne = new TravelTimeEntity(null, "002", "First entity", OffsetDateTime.parse(pubDate), OffsetDateTime.parse(retDate), "type", 200, 5, 100);
        TravelTimeEntity  x = new TravelTimeEntity(null, "002", "First entity", OffsetDateTime.parse(pubDate2), OffsetDateTime.parse(retDate), "type", 200, 5, 100);
        TravelTimeEntity  y = new TravelTimeEntity(null, "002", "First entity", OffsetDateTime.parse(pubDate3), OffsetDateTime.parse(retDate), "type", 200, 5, 100);
        TravelTimeEntity  z = new TravelTimeEntity(null, "002", "First entity", OffsetDateTime.parse(pubDate4), OffsetDateTime.parse(retDate), "type", 200, 5, 100);

        repo.save(entityOne).block();
        repo.save(x).block();
        repo.save(y).block();
        repo.save(z).block();

        repo.getFeatureDateRange(OffsetDateTime.parse(pubDate2), OffsetDateTime.parse(pubDate4))
                .as(StepVerifier::create)
                .expectNextCount(3)
                .verifyComplete();
    }
}
