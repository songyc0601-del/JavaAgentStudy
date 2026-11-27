package dev.javaagentstudy.stage01.exercise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.javaagentstudy.common.testing.FakeChatModel;
import dev.javaagentstudy.stage01.Stage01Application;
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
 * 模型持续返回不可解析内容时的离线集成测试（接缝：HTTP 入口）。
 */
@SpringBootTest(classes = {Stage01Application.class, ExerciseGenerationFailureTest.FakeModelConfig.class})
@AutoConfigureMockMvc
class ExerciseGenerationFailureTest {

    @TestConfiguration
    static class FakeModelConfig {

        @Bean
        @Primary
        FakeChatModel fakeChatModel() {
            return FakeChatModel.replyingWith("这不是合法的 JSON");
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FakeChatModel fakeChatModel;

    @Test
    void returnsStructuredErrorAfterValidationRetriesExhausted() throws Exception {
        fakeChatModel.clearRecordedPrompts();

        mockMvc.perform(post("/api/exercises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topic\":\"集合框架\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("EXERCISE_GENERATION_FAILED"));

        // 校验失败后应带着错误信息重试，因此模型被调用不止一次。
        assertThat(fakeChatModel.recordedPrompts().size()).isGreaterThan(1);
    }
}
