package org.datastax.simulacra.ai;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * An interface for using basic function calling w/ the OpenAI API, but can be adapted
 * to other LLMs like Llama with a little bit of work.
 * <p>
 * <code>&#64;FunctionResponse</code> is used to annotate the response class, which represents
 * the response from the LLM. The response class must be a record, and can be treated like any
 * other Jackson JSON DTO.
 * <ul>
 *     <li>You can use &#64;JsonProperty(required = true) to ensure that a field is required.</li>
 *     <li>You can use &#64;JsonPropertyDescription to add a description to a field.</li>
 * </ul>
 * There is also a special annotation, <code>&#64;EnumType</code>, which can be used to
 * ensure a field is one of some set of values. This is useful for enumberations that
 * are not known at compile-time.
 * <pre>
 * &#64;FunctionResponse
 * private record Response(
 *     &#64;EnumType
 *     &#64;JsonProperty(required = true)
 *     String question,
 *
 *     &#64;JsonPropertyDescription("The answer to the above question")
 *     int answer
 * ) {}
 *
 * public void functionCalling(Example) {
 *     // Can be a list of anything, such as ints, strings, etc.
 *     var questions = List.of("1 + 3", "2 + 2", "e ^ (i * pi) + 5");
 *
 *     var prompt = """
 *         Respond with the hardest question.
 *     """;
 *
 *     // The enum provider lists must be provided in the same order as the
 *     // enum fields in the response class.
 *     var response = openAIService.query(prompt, Response.class, questions)
 *      .join();
 *
 *     assert "e ^ (i * pi) + 5".equals(response.question);
 *     assert 4 == response.answer;
 * }</pre>
 */
public interface FunctionService {
    <T> CompletableFuture<T> query(String prompt, Class<T> response, Collection<?>...providers);

    static FunctionService getDefault() {
        return OpenAIService.INSTANCE;
    }
}
