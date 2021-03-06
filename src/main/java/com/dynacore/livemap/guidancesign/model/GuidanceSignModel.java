package com.dynacore.livemap.guidancesign.model;

import com.dynacore.livemap.core.model.Feature;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.*;

import static java.util.stream.Collectors.toList;

@Getter @Setter
public class GuidanceSignModel extends Feature {

    @JsonIgnore
    public String getName() {
        return properties.getName();
    }

    @JsonIgnore
    public LocalDateTime getPubDate() {
        return properties.getPubDate();
    }

    @JsonIgnore
    public boolean getRemoved() {
        return properties.removed;
    }

    @JsonIgnore
    public String getState() {
        return properties.state;
    }

    private PropertiesImpl properties;
    @JsonProperty("properties")
    private void unpackNested(Map<String, Object> prop) throws IllegalStateException {
        List<InnerDisplayModel> childDisplays = ((ArrayList<LinkedHashMap<String, String>>) prop.get("ParkingguidanceDisplay")).stream()
                .map(dispMap -> new InnerDisplayModel.Builder()
                        .id(UUID.fromString(dispMap.get("Id")))
                        .description(dispMap.get("Description"))
                        .output(dispMap.get("Output"))
                        .outputDescription(dispMap.get("OutputDescription"))
                        .type(dispMap.get("Type"))
                        .build())
                .collect(toList());

        String temp = (String) prop.get("PubDate");

        properties = new PropertiesImpl.Builder()
                .name((String) prop.get("Name"))
                .pubDate(LocalDateTime.parse(temp.substring(0, temp.length() - 1)))
                .type((String) prop.get("Type"))
                .removed(Boolean.valueOf((String) prop.get("Removed")))
                .state((String) prop.get("State"))
                .innerDisplayModelList(childDisplays)
                .build();
    }

}
