package dev.javaagentstudy.stage01.live;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.javaagentstudy.stage01.Stage01Application;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

/**
 * 真实大模型端到端验收测试（接缝：HTTP 入口，与离线测试使用同一组接口）。
 *
 * <p>与离线测试的区别：不注入 FakeChatModel，直连 OPENAI_* 环境变量（或根目录 .env）配置的真实端点，
 * 会产生网络请求与 Token 费用，所以带 live-llm 标签，默认 mvn test 不会运行。
 * 显式运行：mvn -pl stage-01-chat-and-structured-output -am test -Dsurefire.excludedGroups= -Dgroups=live-llm
 * -Dtest=LiveLlmEndToEndTest -Dsurefire.failIfNoSpecifiedTests=false
 * （必须带 -Dtest：否则 common 模块里故意必败的 LiveLlmExclusionGuardTest 也会被运行）
 *
 * <p>断言只检查结构与非空，不比较模型原文，因为真实模型每次输出都不同。
 */
@Tag("live-llm")
// 测试目录下的 application.properties 把 api-key 设成了占位值；这里用 properties 属性（优先级更高）
// 改回读取真实的 OPENAI_API_KEY，否则会带着占位密钥去请求真实端点。
@SpringBootTest(classes = Stage01Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.ai.openai.api-key=${OPENAI_API_KEY:}")
class LiveLlmEndToEndTest {

    /** 真实模型响应较慢，给足超时时间。 */
    private static final Duration TIMEOUT = Duration.ofSeconds(120);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @LocalServerPort
    private int port;

    private WebClient client;

    @BeforeEach
    void createClient() {
        client = WebClient.create("http://localhost:" + port);
    }

    @Test
    void syncExplanationReturnsNonEmptyTextFromRealModel() throws Exception {
        String body = client.post()
                .uri("/api/explanations")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"topic\":\"集合框架\",\"focus\":\"ArrayList 与 LinkedList 的区别\"}")
                .retrieve()
                .bodyToMono(String.class)
                .block(TIMEOUT);

        JsonNode json = objectMapper.readTree(body);
        assertThat(json.get("topic").asText()).isEqualTo("集合框架");
        assertThat(json.get("explanation").asText()).isNotBlank();
    }

    @Test
    void streamExplanationPushesMultipleChunksFromRealModel() {
        // WebClient 按 SSE 规范逐事件解析，collectList 把所有分片收集起来。
        List<String> chunks = client.post()
                .uri("/api/explanations/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue("{\"topic\":\"集合框架\",\"focus\":\"ArrayList 与 LinkedList 的区别\"}")
                .retrieve()
                .bodyToFlux(String.class)
                .collectList()
                .block(TIMEOUT);

        // 真实模型的分片数量与内容不固定：只断言确实是“多段推送”，且拼接后有实际内容。
        assertThat(chunks).isNotNull().hasSizeGreaterThan(1);
        assertThat(String.join("", chunks)).isNotBlank();
    }

    @Test
    void exerciseGenerationReturnsStronglyTypedExerciseFromRealModel() throws Exception {
        String body = client.post()
                .uri("/api/exercises")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"topic\":\"集合框架\",\"difficulty\":\"BEGINNER\",\"focus\":\"ArrayList 与 LinkedList 的区别\"}")
                .retrieve()
                .bodyToMono(String.class)
                .block(TIMEOUT);

        // 真实模型的题目文字每次不同：只断言强类型结构完整、关键字段非空。
        JsonNode json = objectMapper.readTree(body);
        assertThat(json.get("title").asText()).isNotBlank();
        assertThat(json.get("topic").get("name").asText()).isNotBlank();
        assertThat(json.get("difficulty").asText()).isEqualTo("BEGINNER");
        assertThat(json.get("description").asText()).isNotBlank();
        assertThat(json.get("hints").isArray()).isTrue();
        assertThat(json.get("referenceAnswerPoints").size()).isGreaterThan(0);
    }

    @Test
    void invalidInputReturns400WithoutCallingRealModel() {
        // 空白 topic 在进入 Service 前就被校验拦截，返回 400，不会产生模型调用与费用。
        HttpStatusCode status = client.post()
                .uri("/api/explanations")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"topic\":\"  \"}")
                .exchangeToMono(response -> Mono.just(response.statusCode()))
                .block(TIMEOUT);

        assertThat(status.value()).isEqualTo(400);
    }
}
