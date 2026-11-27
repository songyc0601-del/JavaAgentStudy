package dev.javaagentstudy.stage01.explain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import dev.javaagentstudy.common.testing.FakeChatModel;
import dev.javaagentstudy.stage01.Stage01Application;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * SSE 流式概念讲解的离线集成测试（接缝 1：HTTP 入口）。
 *
 * <p>启动完整 Spring 上下文，只把最外层 ChatModel 换成回放预设分片的假模型，
 * 再通过 HTTP 请求验证 SSE 响应的类型、分片顺序与拼接文本。
 */
@SpringBootTest(classes = {Stage01Application.class, ConceptExplanationStreamApiTest.StreamingModelConfig.class})
@AutoConfigureMockMvc
class ConceptExplanationStreamApiTest {

    /** 测试专用配置：用回放三段分片的假模型顶替真实大模型。 */
    @TestConfiguration
    static class StreamingModelConfig {

        @Bean
        @Primary
        FakeChatModel fakeChatModel() {
            return FakeChatModel.streamingChunks("集合", "框架", "入门");
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FakeChatModel fakeChatModel;

    // Spring 会在同一个测试类的多个测试之间复用同一个假模型，每个测试前先清空记录。
    @BeforeEach
    void resetRecordedPrompts() {
        fakeChatModel.clearRecordedPrompts();
    }

    @Test
    void streamsAllChunksInOrderAsServerSentEvents() throws Exception {
        // 执行：提交合法请求，SSE 响应是异步的，先确认异步处理已启动
        MvcResult started = mockMvc.perform(post("/api/explanations/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"topic\":\"集合框架\",\"focus\":\"ArrayList 与 LinkedList 的区别\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        // 验证：异步分发完成后返回 200，且 Content-Type 为 text/event-stream
        String body = mockMvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        // 验证：data 行按预设顺序出现，拼接后等于完整文本
        List<String> chunks = Arrays.stream(body.split("\n"))
                .filter(line -> line.startsWith("data:"))
                // SSE 规范规定：data: 后面的一个前导空格属于字段分隔符，客户端解析时要去掉它。
                .map(line -> line.substring("data:".length()))
                .map(data -> data.startsWith(" ") ? data.substring(1) : data)
                .toList();
        assertThat(chunks).containsExactly("集合", "框架", "入门");
        assertThat(String.join("", chunks)).isEqualTo("集合框架入门");

        // 验证：模型被流式调用了一次，提示词包含 topic 与 focus
        assertThat(fakeChatModel.recordedPrompts()).hasSize(1);
        assertThat(fakeChatModel.recordedPrompts().get(0).getContents())
                .contains("集合框架")
                .contains("ArrayList 与 LinkedList 的区别");
    }
}
