package com.dynacore.livemap.traveltime.repo;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.annotation.Profile;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import static org.springframework.data.r2dbc.query.Criteria.where;

import reactor.bool.BooleanUtils;
import reactor.core.publisher.Mono;

@Profile("traveltime")
@Repository("travelTimeRepository")
public class TravelTimeRepo {

    private DatabaseClient databaseClient;
    private final static Logger logger = LoggerFactory.getLogger(TravelTimeRepo.class);

    public TravelTimeRepo(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    public Mono<Boolean> isUnique(TravelTimeEntity entity) {
        Mono<Boolean> result = null;
        try {
            result = databaseClient.select().from(TravelTimeEntity.class)
                    .matching(where("id")
                            .is(entity.getId())
                            .and("pubDate")
                            .is(entity.getPubDate()))
                    .fetch()
                    .first()
                    .hasElement();
        } catch (Exception e) {
            logger.error("Can't save road information to DB: " + e.toString());
            return Mono.error(new Exception());
        }
        return  BooleanUtils.not(result);
    }

    public Mono<Integer> save(TravelTimeEntity entity) {

            return databaseClient.insert()
                    .into(TravelTimeEntity.class)
                    .using(entity)
                    .fetch()
                    .rowsUpdated()
                    .doOnError(e -> logger.error("Error writing to db:  ", e));
    }

    public Mono<TravelTimeEntity> getLastStored(TravelTimeEntity entity) {
        return databaseClient.execute(
                "     SELECT id, name, pub_date, retrieved_from_third_party, type, length, travel_time, velocity \n" +
                        "     FROM public.travel_time_entity\n" +
                        "\t   WHERE pub_date=(\n" +
                        "                SELECT MAX(pub_date) FROM public.travel_time_entity WHERE id='" + entity.getId() + "');")
                .as(TravelTimeEntity.class)
                .fetch()
                .first();
    }

    public Mono<Boolean> didPropertiesChange(TravelTimeEntity entity) {
        return getLastStored(entity)
                .map(storedEntity -> {
                    boolean changed = false;
                    if (storedEntity.getLength() != entity.getLength()) {
                        changed = true;
                        logger.trace("--Length changed");
                        logger.trace("----old: " + storedEntity.getLength());
                        logger.trace("----new: " + entity.getLength());
                    }
                    if (storedEntity.getTravel_time() != entity.getTravel_time()) {
                        changed = true;
                        logger.trace("--Traveltime changed");
                        logger.trace("----old: " + storedEntity.getTravel_time());
                        logger.trace("----new: " + entity.getTravel_time());
                    }
                    if (storedEntity.getVelocity() != entity.getVelocity()) {
                        changed = true;
                        logger.trace("--Velocity changed: ");
                        logger.trace("----old: " + storedEntity.getVelocity());
                        logger.trace("----new: " + entity.getVelocity());
                    }
                    return changed;
                });
    }

}
