package dev.javaagentstudy.stage01.explain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import dev.javaagentstudy.common.testing.FakeChatModel;
import dev.javaagentstudy.stage01.Stage01Application;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * SSE 流式讲解的真实 HTTP 客户端离线测试（接缝 1：HTTP 入口）。
 *
 * <p>启动真实内嵌 Web 服务器（随机端口），用 WebClient 作为 SSE 客户端逐段消费响应，
 * 只把最外层 ChatModel 换成假模型。
 */
@SpringBootTest(classes = {Stage01Application.class, ConceptExplanationStreamHttpClientTest.StreamingModelConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConceptExplanationStreamHttpClientTest {

    /** 测试专用配置：用回放三段分片的假模型顶替真实大模型。 */
    @TestConfiguration
    static class StreamingModelConfig {

        @Bean
        @Primary
        FakeChatModel fakeChatModel() {
            return FakeChatModel.streamingChunks("集合", "框架", "入门");
        }
    }

    @LocalServerPort
    private int port;

    @Autowired
    private FakeChatModel fakeChatModel;

    @BeforeEach
    void resetRecordedPrompts() {
        fakeChatModel.clearRecordedPrompts();
    }

    @Test
    void sseClientReceivesChunksInOrder() {
        // WebClient 按 SSE 规范解析事件，已自动去掉 data: 后的分隔空格。
        List<String> chunks = WebClient.create("http://localhost:" + port)
                .post()
                .uri("/api/explanations/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue("{\"topic\":\"集合框架\"}")
                .retrieve()
                .bodyToFlux(String.class)
                .collectList()
                .block(Duration.ofSeconds(10));

        assertThat(chunks).containsExactly("集合", "框架", "入门");
        assertThat(String.join("", chunks)).isEqualTo("集合框架入门");
        assertThat(fakeChatModel.recordedPrompts()).hasSize(1);
    }
}
