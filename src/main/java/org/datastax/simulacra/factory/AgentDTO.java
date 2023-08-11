package org.datastax.simulacra.factory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.datastax.simulacra.ai.EnumType;
import org.datastax.simulacra.ai.FunctionResponse;

import java.util.List;

@FunctionResponse
public record AgentDTO(
    @JsonProperty(required = true)
    String name,

    @JsonProperty(required = true)
    Integer age,

    String house,

    @EnumType
    @JsonProperty(required = true)
    String startSubArea,

    @EnumType
    @JsonPropertyDescription("The agent's job or school area, if any. If not, leave blank.")
    String jobOrSchoolPlace,

    @JsonProperty(required = true)
    List<String> traits,

    @JsonProperty(required = true)
    List<String> memories,

    @JsonProperty(required = true)
    String circRhythm,

    @JsonProperty(required = true)
    String currentAction
) {}
