package org.datastax.simulacra.ai;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.theokanning.openai.OpenAiError;
import com.theokanning.openai.OpenAiHttpException;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionRequest.ChatCompletionRequestBuilder;
import com.theokanning.openai.completion.chat.ChatCompletionRequest.ChatCompletionRequestFunctionCall;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatFunction;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.embedding.EmbeddingResult;
import com.theokanning.openai.service.FunctionExecutor;
import io.reactivex.Single;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.HttpException;
import retrofit2.http.Body;
import retrofit2.http.POST;

import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static com.theokanning.openai.service.OpenAiService.*;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toMap;
import static org.datastax.simulacra.utils.Utils.*;
import static org.datastax.simulacra.ai.IOExecutor.defaultSupplyAsync;
import static org.datastax.simulacra.logging.HomemadeLogger.err;
import static org.datastax.simulacra.logging.HomemadeLogger.log;

public enum OpenAIService implements ChatService, EmbeddingService, FunctionService {
    INSTANCE;

    private static final int MAX_RETRIES = 1;

    private final ObjectMapper mapper = defaultObjectMapper();
    private final MyOpenAiApi api = createApi();

    {
        mapper.enable(JsonParser.Feature.ALLOW_COMMENTS);
    }

    @Override
    public CompletableFuture<String> query(String text) {
        return defaultSupplyAsync(() -> queryNormalSync(text));
    }

    @Override
    public <T> CompletableFuture<T> query(String text, Class<T> body, Collection<?> ...providers) {
        return defaultSupplyAsync(() -> queryFunctionSync(text, body, providers));
    }

    @Override
    public CompletableFuture<List<Float>> embed(String text) {
        return defaultSupplyAsync(() -> embedSync(text));
    }

    @Override
    public CompletableFuture<List<List<Float>>> embed(List<String> text) {
        return awaitAll(map(text, t -> defaultSupplyAsync(() -> embedSync(t))));
    }

    private List<Float> embedSync(String text) {
        var request = EmbeddingRequest.builder()
            .input(List.of(compactText(text)))
            .model("text-embedding-ada-002")
            .build();

        log("Making embedding request");

        return execute(api.createEmbeddings(request))
            .getData()
            .get(0)
            .getEmbedding()
            .stream()
            .map(Double::floatValue)
            .toList();
    }

    private String queryNormalSync(String prompt) {
        return query(prompt, x -> obj2JsonString(x.build())).getContent();
    }

    private <T, R> R queryFunctionSync(String text, Class<T> body, Collection<?>[] providers) {
        var annotation = body.getAnnotation(FunctionResponse.class);

        var fnName = body.getSimpleName();
        var fnDesc = annotation.desc().isEmpty() ? null : annotation.desc();

        var function = ChatFunction.builder()
            .name(fnName)
            .description(fnDesc)
            .executor(body, x -> x)
            .build();

        var fnList = singletonList(function);
        var fnExecutor = new FunctionExecutor(fnList, mapper);

        var fnCall = query(text, builder -> {
            var request = builder
                .functions(fnExecutor.getFunctions())
                .functionCall(new ChatCompletionRequestFunctionCall(fnName))
                .build();

            if (providers.length == 0) {
                return obj2JsonString(request);
            }

            var json = obj2JsonNode(request);
            populateEnumFields(json, body, providers);
            return writeJsonAsString(json);
        }).getFunctionCall();

        return fnExecutor.execute(fnCall);
    }

    private void populateEnumFields(JsonNode json, Class<?> clazz, Collection<?>[] providers) {
        var properties = json
            .get("functions")
            .get(0)
            .get("parameters")
            .get("properties");

        var ref = new Object() {
            int providerIndex = 0;
        };

        var enumFields = Arrays.stream(clazz.getRecordComponents())
            .filter(f -> f.getAnnotation(EnumType.class) != null)
            .collect(toMap(
                RecordComponent::getName,
                f -> {
                    var values = providers[ref.providerIndex++];

                    return map(values, x -> (
                        (x instanceof String) ? "\"" + x + "\"" : x
                    ));
                }
            ));

        enumFields.forEach((field, values) -> {
            var valuesAsNodes = map(values, v -> TextNode.valueOf(v.toString()));
            var fieldNode = (ObjectNode) properties.get(field);
            fieldNode.putArray("enum").addAll(valuesAsNodes);
        });
    }

    private ChatMessage query(String prompt, Function<ChatCompletionRequestBuilder, String> buildFn) {
        var compactPrompt = compactText(prompt);
        var msg = List.of(new ChatMessage("user", compactPrompt));

        var builder = ChatCompletionRequest.builder()
            .model("gpt-3.5-turbo")
            .messages(msg);

        var requestString = buildFn.apply(builder);

//        try {
//            ObjectMapper objectMapper = new ObjectMapper();
//            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
//            Object jsonObject = objectMapper.readValue(requestString, Object.class);
//            String prettyPrintedJson = objectMapper.writeValueAsString(jsonObject);
//            System.out.println(prettyPrintedJson);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        var body = RequestBody.create(
            MediaType.parse("application/json; charset=utf-8"), requestString
        );

        log("--------------------------------------");
        log(compactPrompt);
        log("--------------------------------------");

        int retries = 0;

        while (retries <= MAX_RETRIES) {
            try {
                return execute(api.createChatCompletion(body))
                    .getChoices()
                    .get(0)
                    .getMessage();
            } catch (Exception e) {
                err("Failed to make chat completions request", e);
            } finally {
                retries++;
            }
        }
        throw new RuntimeException("Failed to complete chat completions request (" + compactPrompt + ")");
    }

    private String compactText(String prompt) {
        return prompt.replaceAll("\\s+", " ").trim();
    }

    private MyOpenAiApi createApi() {
        var client = defaultClient(System.getenv("OPENAI_TOKEN"), Duration.ofSeconds(15));

        return defaultRetrofit(
            client, mapper
        ).create(MyOpenAiApi.class);
    }

    @SuppressWarnings("DataFlowIssue")
    public static <T> T execute(Single<T> apiCall) {
        try {
            return apiCall.blockingGet();
        } catch (HttpException e) {
            if (e.response() == null) {
                throw e;
            }

            try (var errorBody = e.response().errorBody()) {
                if (errorBody == null) {
                    throw e;
                }

                var error = readJsonTree(errorBody.string(), new TypeReference<OpenAiError>() {});
                throw new OpenAiHttpException(error, e, e.code());
            } catch (IOException ex) {
                throw e;
            }
        }
    }

    private interface MyOpenAiApi {
        @POST("/v1/chat/completions")
        Single<ChatCompletionResult> createChatCompletion(@Body RequestBody request);

        @POST("/v1/embeddings")
        Single<EmbeddingResult> createEmbeddings(@Body EmbeddingRequest request);
    }

    private String obj2JsonString(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private JsonNode obj2JsonNode(Object obj) {
        return mapper.valueToTree(obj);
    }
}
