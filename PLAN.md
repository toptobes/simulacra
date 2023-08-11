## Main loop
The core of the simulation will be a main loop controlled by a monotonic clock that'll fire off a
"moment" at constant intervals.

## Moments
A "moment" represents some agent-specific moment of time. There'll be three types of moments: 
reactive, proactive, and reflective.

  - Reactive moments will be by far the majority of moments, where the agent notices its
    surroundings and updates its memory stream.
    1. Notice surrounding environment and update local map
    2. Make observations and react accordingly
       - If reaction, then re-plan rest of day
       - If interaction, generate dialogue
       - Else, act out plan
    3. If importance threshold reached, reflect
   
  - Proactive moments occur when an agent wakes up.
    1. Plan out day in detail

  - Reflective moments occur when an agent is going to sleep;
    1. Reflect over past day
    2. Synthesis what the day's plan ended up being in broad strokes

## Environment
There will be a central undirected graph of "areas", which are nodes which each carry a tree of 
"subareas" whose leaf nodes represent "objects" that may be interacted with.

Each agent will contain their own internal non-omniscient knowledge of the subarea. Every reactive
moment, its copy of its current subarea will be fully updated. It does introduce "race" conditions
as to who could use something first, but that's just life, eh?

## Memory stream
Agent's memory may be stored in an Astra instance w/ schema

```
CREATE TABLE simulacra.memories_by_agent (
  agent_name TEXT PRIMARY KEY,
  created_at INT,
  last_fetch INT,
  importance INT,
  type TINYINT,
  memory TEXT,
  embedding VECTOR<FLOAT, 128>
);
```

## Interaction
An agent will be aware of the people and things in its certain sub-area, updated each reactive
moment.

## Planning
Plans will be made every proactive moment as well as when determined necessary in a reactive
moment. Plans will be recursively generated to create more and more detail, until the timeframe
reaches the granularity of a "moment".

## Reflecting
Reflections will be stored as a memory in the memory stream, w/ the text format

```
<memory here> (because of <comma sep list of dependent memories>)
```

The dependent memories will have some max recursion depth in case of recursive memories.

## Potential optimizations
Employ existence-based-processing to only update the agents that have been interacted with? 
Could be an efficient way to scale up the number of agents, though in reality, it'd be a facade.
