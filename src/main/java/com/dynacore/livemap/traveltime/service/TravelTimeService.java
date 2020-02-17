/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dynacore.livemap.traveltime.service;

import com.dynacore.livemap.core.adapter.GeoJsonAdapter;
import com.dynacore.livemap.core.service.GeoJsonReactorService;
import com.dynacore.livemap.traveltime.domain.TravelTimeMapDTO;
import com.dynacore.livemap.traveltime.domain.TravelTimeFeatureImpl;
import com.dynacore.livemap.traveltime.repo.TravelTimeEntityImpl;
import com.dynacore.livemap.traveltime.repo.TravelTimeRepo;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.bool.BooleanUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import static org.springframework.data.r2dbc.query.Criteria.where;

/**
 * Road traffic traveltime service
 *
 * <p>This service will subscribe to a traffic information data source. It checks the GeoJson for
 * RFC compliance and calculates properties and publishes the new data in a reactive manner. The
 * data is automatically saved in a reactive database (R2DBC) and the last emitted signals are
 * cached for new subscribers.
 */
@Lazy(false)
@Profile("traveltime")
@Service("travelTimeService")
public class TravelTimeService
    extends GeoJsonReactorService<TravelTimeEntityImpl, TravelTimeFeatureImpl, TravelTimeMapDTO> {

  Logger log = LoggerFactory.getLogger(TravelTimeService.class);

  @Autowired
  DatabaseClient client;

  public TravelTimeService(
          TravelTimeServiceConfig config,
          ObjectProvider<GeoJsonAdapter> geoJsonAdapterObjectProvider,
          TravelTimeImporter importer,
          TravelTimeRepo repo,
          TravelTimeDTODistinct entityDtoDistinct,
          TravelTimeFeatureDistinct featureDistinct
      )
      throws JsonProcessingException {
    super(config, geoJsonAdapterObjectProvider, importer, repo,  entityDtoDistinct, featureDistinct);

    if (config.isSaveToDbEnabled()) {
      Flux.from(importedFlux)
          .parallel(Runtime.getRuntime().availableProcessors())
          .runOn(Schedulers.parallel())
          .map(TravelTimeEntityImpl::new)
          .map(this::save)
          .subscribe(Mono::subscribe,
                  error -> log.error("Error: " + error)
          );
    }
  }

  public Mono<Void> save(TravelTimeEntityImpl entity) {

    return client
            .select()
            .from(TravelTimeEntityImpl.class)
            .matching(where("id").is(entity.getId()).and("pubDate").is(entity.getPubDate()))
            .fetch()
            .first()
            .hasElement()
            .transform(BooleanUtils::not)
            .filter(Boolean::booleanValue)
         //   .doOnNext(x -> System.out.println("Saving " + entity ) )
            .flatMap( newEntity ->
                    client
                            .insert()
                            .into(TravelTimeEntityImpl.class)
                            .using(entity)
                            .fetch().rowsUpdated())
            .then();
  }
}
