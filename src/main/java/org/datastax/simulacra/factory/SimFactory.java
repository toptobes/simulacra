package org.datastax.simulacra.factory;

import com.fasterxml.jackson.core.type.TypeReference;
import org.datastax.simulacra.agents.Agent;
import org.datastax.simulacra.agents.AgentRegistry;
import org.datastax.simulacra.ai.EnumType;
import org.datastax.simulacra.ai.FunctionClass;
import org.datastax.simulacra.ai.FunctionService;
import org.datastax.simulacra.environment.Area;
import org.datastax.simulacra.environment.SubArea;
import org.datastax.simulacra.environment.WorldMap;
import org.datastax.simulacra.memorystream.AstraMemoryStream;
import org.datastax.simulacra.memorystream.MemoryEntity;
import org.datastax.simulacra.memorystream.MemoryType;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toMap;
import static org.datastax.simulacra.Utils.*;

public class SimFactory {
    public static String CUSTOM_AGENTS_PATH = "simulacra/config/agents.yaml";
    public static String CUSTOM_ENVIRONMENT_PATH = "simulacra/config/environment.yaml";

    public static String AGENTS_CACHE_PATH = "simulacra/cache/agents.yaml";
    public static String ENVIRONMENT_CACHE_PATH = "simulacra/cache/environment.yaml";

    public static void loadSimulation(int roughNumAgents) {
        WorldMap.GLOBAL.addAreas(areasFromYaml(CUSTOM_ENVIRONMENT_PATH));
        var agents = agentsFromYaml(CUSTOM_AGENTS_PATH);
        System.out.printf("Found %d agents from custom config%n", agents.size());

        WorldMap.GLOBAL.addAreas(areasFromYaml(ENVIRONMENT_CACHE_PATH));
        var cachedAgents = agentsFromYaml(AGENTS_CACHE_PATH);
        System.out.printf("Found %d agents from generated cache%n", cachedAgents.size());

        var cons = generateAgents(roughNumAgents - agents.size() - cachedAgents.size());

        System.out.printf("Generated %d agents and %d areas%n", cons.snd().size(), cons.fst().size());

        saveToCache(cons.fst(), ENVIRONMENT_CACHE_PATH);
        saveToCache(cons.snd(), AGENTS_CACHE_PATH);

        WorldMap.GLOBAL.addAreas(cons.fst());
        var newAgents = dtos2agents(cons.snd());

        cat(newAgents, cachedAgents, agents).forEach(AgentRegistry::register);
    }

    @FunctionClass
    private record HouseDTO(
        String name,
        String description,
        List<String> residents,
        String residentsDynamic,
        List<SubArea> subAreas
    ) {}

    @FunctionClass
    private record PlaceDTO(
        String name,
        String description,
        @EnumType
        String category,
        int maxWorkers,
        List<SubArea> subAreas
    ) {}

    private static Pair<List<Area>, List<AgentDTO>> generateAgents(int roughNumAgents) {
        var numPlacesForEmployment = Math.max(0, roughNumAgents / 12);
        var roughNumFamilies = Math.max(0, roughNumAgents / 3);

        var placesForEmployment = IntStream.range(0, numPlacesForEmployment)
            .mapToObj(i -> generatePlaceOfEmployment())
            .toList();

        var workersPerPlace = associateWith(map(placesForEmployment, p -> normalizeName(p.name())), p -> 0);
        var normalizedPlaceNames = map(placesForEmployment, p -> normalizeName(p.name()));

        var families = IntStream.range(0, roughNumFamilies)
            .mapToObj(i -> generateAgentFamily(normalizedPlaceNames, workersPerPlace))
            .collect(toMap(Pair::fst, Pair::snd));

        var areas = cat(families.keySet(), placesForEmployment);
        var agents = flatten(families.values());

        return cons(areas, agents);
    }

    private static Area generatePlaceOfEmployment() {
        try {
            var prompt = """
                    Generate an imaginative place of employment for some amount of people).
                    Use generic item statuses like "idle", "unused" or "being used by two people" or something.
                    
                    Rough example (add more subareas/items in yours IF APPROPRIATE):
                    {
                      "name": "Dr. Lin's Family Practice",
                      "description": "A small, friendly local doctor's office where medical service is provided to the community",
                      "category": "hospital",
                      "maxWorkers": 3,
                      "subareas": [
                        {
                          "name": "waiting area",
                          "items": [
                            {
                              "name": "chairs",
                              "status": "unoccupied"
                            },
                            {
                              "name": "magazine",
                              "status": "idle"
                            },
                            {
                              "name": "reception desk",
                              "status": "unoccupied"
                            }
                          ]
                        },
                        {
                          "name": "examination room",
                          "items": [
                            {
                              "name": "examination table",
                              "status": "empty"
                            },
                            {
                              "name": "medical equipment",
                              "status": "unused"
                            }
                          ]
                        }
                      ]
                    }
                    
                    Other examples include a restaurant or a hospital or something, it's not limited to anything, but
                    prefer businesses on the smaller size (5-10 employees max, if not less).
                    
                    Be realistic and normal, not dramatic.

                    Follow the result format strictly w/ no more, no less detail.
                """;

            return FunctionService.getDefault().query(prompt, PlaceDTO.class, Area.Category.excluding(Area.Category.HOUSE))
                .thenApply(place -> (
                    Area.createArea(place.name(), place.subAreas, place.description(), place.category)
                )).join();
        } catch (Exception e) {
            e.printStackTrace();
            return generatePlaceOfEmployment();
        }
    }

    private static Pair<Area, List<AgentDTO>> generateAgentFamily(List<String> places, Map<String, Integer> workersPerPlace) {
        try {
            var housePrompt = """
                Generate an imaginative house for some amount of people (could be family, could be roommates, could be single/couple).
                Use generic item statuses like "idle", "unused" or "being used by two people" or something.
                
                Residents must have full names. Use more creative names than just the super common ones like "smith".
                House name should be quite creative and unique too.
                
                Rough example (add more subareas/items in yours IF APPROPRIATE):
                {
                    "name": "Lin Family's House",
                    "description": "A modest sized house the 3-member Lin family happily resides",
                    "residents": ["John Lin", "Mei Lin", "Eddie Lin"],
                    "residentsDynamic": "John Lin and Mei Lin are married, and Eddie Lin is their son",
                    "subAreas": [
                        {
                            "name": "master bedroom",
                            "items": [
                                {
                                    "name": "bed",
                                    "status": "occupied by both parents"
                                },
                                {
                                    "name": "computer",
                                    "status": "idle"
                                },
                                {
                                    "name": "closet",
                                    "status": "idle"
                                }
                            ]
                        },
                        {
                            "name": "garden",
                            "items": []
                        }
                    ]
                }
                
                Be realistic and normal, not dramatic.
                        
                Follow the result format strictly w/ no more, no less detail.
            """;

            return FunctionService.getDefault().query(housePrompt, HouseDTO.class)
                .thenApply(house -> {
                    var area = Area.createArea(house.name(), house.subAreas, house.description(), Area.Category.HOUSE);

                    Function<String, String> agentPromptFn = (residentName) -> """
                        Generate an agent for me whose name is %s, and who lives with %s.
                        
                        The dynamic/hierarchy of the house is as follows: %s.
                        
                        They live in the house called '%s', which contains the subareas %s.
                        
                        Please keep the memories about only very import things only, such as living conditions and work.
                        The memories should be somewhat akin to the ones in the example in terms of subject and conciseness. Nothing long and dramatic.
                        But they can still have their own touch, just don't go overboard, and no more than 4-5 relevant memories (that shape who he is) max.
                        Please don't ramble on about items/furniture in the house
                        
                        Example of such an agent:
                        {
                            "name": "John Lin",
                            "age": 35,
                            "traits": ["kind", "hardworking", "loving"],
                            "startSubArea": "master bedroom",
                            "job": "Dr. Lin's Family Practice",
                            "currentAction": "sleeping",
                            "circRhythm": "7am to 11pm",
                            "memories": [
                                "John Lin is a local doctor @ 'Dr. Lin's Family Practice' who loves to help people",
                                "He is always looking for ways to make the process of getting medication easier for his customers",
                                "John Lin is living with his wife, Mei Linn, who teaches @ Largerville Public School, and son, Eddy Lin, who is a student studying music theory",
                                "John Lin loves his family very much"
                            ]
                        }
                        
                        Follow the result format strictly w/ no more, no less detail. Make sure JSON result is valid. (esp mem list)
                    """.formatted(
                        residentName,
                        house.residents(),
                        house.residentsDynamic(),
                        house.name(),
                        map(house.subAreas(), SubArea::name)
                    );

                    var placesOfEmployment = workersPerPlace.entrySet().stream().map(entry -> (
                        "%s (%d/%d workers)".formatted(entry.getKey(), entry.getValue(), places.size())
                    )).toList();

                    var agents = asyncListProcessor(house.residents)
                        .pipeSync(agentPromptFn)
                        .pipe(agentPrompt -> (
                            FunctionService.getDefault().query(agentPrompt, AgentDTO.class, map(house.subAreas(), SubArea::name), placesOfEmployment)
                        ))
                        .pipeSync(dto -> (
                            new AgentDTO(dto.name(), dto.age(), house.name(), dto.startSubArea(), substringBefore(dto.jobOrSchoolPlace(), " ("), dto.traits(), dto.memories(), dto.circRhythm(), dto.currentAction())
                        ))
                        .peekSync(dto -> {
                            if (workersPerPlace.containsKey(dto.jobOrSchoolPlace())) {
                                workersPerPlace.put(dto.jobOrSchoolPlace(), workersPerPlace.get(dto.jobOrSchoolPlace()) + 1);
                            }
                        })
                        .get();

                    return cons(area, agents);
                }).join();
        } catch (Exception e) {
            e.printStackTrace();
            return generateAgentFamily(places, workersPerPlace);
        }
    }

    private static List<Agent> dtos2agents(List<AgentDTO> dtos) {
        return map(dtos, SimFactory::dto2agent);
    }

    private static Agent dto2agent(AgentDTO dto) {
        var memories = dto.memories().stream().map(memory -> (
            MemoryEntity.from(dto.name(), 8, MemoryType.OBSERVATION, memory).join()
        )).toList();

        var stream = new AstraMemoryStream();
        stream.save(dto.name(), memories);

        var area = WorldMap.GLOBAL.findArea(dto.house());
        var subarea = area.findSubarea(dto.startSubArea());

        return new Agent(
            dto.name(), dto.age(), dto.traits(), stream, subarea, dto.currentAction(), dto.circRhythm(), area
        );
    }

    private static List<Agent> agentsFromYaml(String path) {
        return dtos2agents(readYaml(path, new TypeReference<>() {}, List.of()));
    }

    private static List<Area> areasFromYaml(String path) {
        return readYaml(path, new TypeReference<>() {}, List.of());
    }

    private static void saveToCache(List<?> dtos, String path) {
        if (!dtos.isEmpty()) {
            writeYaml(path, dtos, true);
        }
    }
}
