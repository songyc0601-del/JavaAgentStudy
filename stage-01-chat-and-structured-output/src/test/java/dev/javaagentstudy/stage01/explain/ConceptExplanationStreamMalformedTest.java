package dev.javaagentstudy.stage01.explain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

/**
 * SSE 流式讲解对非法 JSON 请求体的离线测试（接缝 1：HTTP 入口）。
 *
 * <p>请求体不是合法 JSON 属于调用方错误，必须返回 400 且不触发模型的流式调用。
 */
@SpringBootTest(classes = {Stage01Application.class, ConceptExplanationStreamMalformedTest.StreamingModelConfig.class})
@AutoConfigureMockMvc
class ConceptExplanationStreamMalformedTest {

    /** 测试专用配置：若 400 场景里假模型被调用，记录数就不为 0。 */
    @TestConfiguration
    static class StreamingModelConfig {

        @Bean
        @Primary
        FakeChatModel fakeChatModel() {
            return FakeChatModel.streamingChunks("集合", "框架");
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
    void malformedJsonReturns400WithoutCallingModel() throws Exception {
        mockMvc.perform(post("/api/explanations/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));

        assertThat(fakeChatModel.recordedPrompts()).isEmpty();
    }
}
