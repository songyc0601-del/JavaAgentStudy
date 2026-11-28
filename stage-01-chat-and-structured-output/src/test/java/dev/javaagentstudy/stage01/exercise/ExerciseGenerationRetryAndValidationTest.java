package dev.javaagentstudy.stage01.exercise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicReference;

import dev.javaagentstudy.common.testing.FakeChatModel;
import dev.javaagentstudy.stage01.Stage01Application;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import reactor.core.publisher.Flux;

/**
 * Exercise 生成的校验重试恢复、强类型非空契约与分支日志集成测试（接缝：HTTP 入口）。
 */
@SpringBootTest(classes = {
        Stage01Application.class,
        ExerciseGenerationRetryAndValidationTest.SwitchableModelConfig.class
})
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class ExerciseGenerationRetryAndValidationTest {

    private static final String VALID_EXERCISE_JSON = """
            {
              "title": "理解 HashMap 扩容",
              "topic": {"name": "集合框架"},
              "difficulty": "BEGINNER",
              "description": "说明 HashMap 负载因子默认 0.75 的含义与触发扩容的条件。",
              "hints": ["回忆数组长度与阈值的关系"],
              "referenceAnswerPoints": ["元素数量超过 capacity * loadFactor 时按 2 倍扩容"]
            }
            """;

    static class SwitchableChatModel implements ChatModel {

        private final AtomicReference<FakeChatModel> delegate =
                new AtomicReference<>(FakeChatModel.replyingWith(VALID_EXERCISE_JSON));

        void use(FakeChatModel model) {
            delegate.set(model);
        }

        FakeChatModel current() {
            return delegate.get();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            return delegate.get().call(prompt);
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return delegate.get().stream(prompt);
        }
    }

    @TestConfiguration
    static class SwitchableModelConfig {

        @Bean
        @Primary
        SwitchableChatModel switchableChatModel() {
            return new SwitchableChatModel();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SwitchableChatModel chatModel;

    @Test
    void recoversWhenModelReturnsInvalidJsonFirstThenValidExercise(CapturedOutput output) throws Exception {
        // 首次返回非法 JSON，第二次返回合法 Exercise JSON，验证 StructuredOutputValidationAdvisor 自动重试恢复。
        FakeChatModel retryingModel = FakeChatModel.replyingInOrder("这不是合法 JSON", VALID_EXERCISE_JSON);
        chatModel.use(retryingModel);

        mockMvc.perform(post("/api/exercises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topic\":\"集合框架\",\"focus\":\"HashMap 扩容\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("理解 HashMap 扩容"))
                .andExpect(jsonPath("$.topic.name").value("集合框架"));

        assertThat(retryingModel.recordedPrompts()).hasSize(2);
        assertThat(output.getOut())
                .contains("接口入口[生成Exercise] POST /api/exercises")
                .contains("服务分支[Exercise生成成功]")
                .contains("接口分支[生成成功返回] POST /api/exercises");
    }

    @Test
    void rejectsSemanticallyEmptyExerciseAndLogsWithoutLeakingModelOutput(CapturedOutput output) throws Exception {
        // JSON 语法合法但 title 为空白且 referenceAnswerPoints 为空，应被 Exercise 构造器拦截并映射为 502。
        String incompleteJson = """
                {
                  "title": "   ",
                  "topic": {"name": "集合框架"},
                  "difficulty": "BEGINNER",
                  "description": "SECRET_RAW_MODEL_OUTPUT_不应出现在日志里",
                  "hints": ["提示"],
                  "referenceAnswerPoints": []
                }
                """;
        chatModel.use(FakeChatModel.replyingWith(incompleteJson));

        mockMvc.perform(post("/api/exercises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topic\":\"集合框架\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("EXERCISE_GENERATION_FAILED"));

        assertThat(output.getOut())
                .contains("服务分支[Exercise格式不合法]")
                .contains("接口分支[生成请求失败] POST /api/exercises：条件=服务层抛出ExerciseFormatException")
                .contains("异常分支[Exercise格式不合法]：条件=捕获ExerciseFormatException")
                .doesNotContain("SECRET_RAW_MODEL_OUTPUT_不应出现在日志里");
    }

    @Test
    void logsControllerFailureBranchWithCauseWhenModelCallFails(CapturedOutput output) throws Exception {
        chatModel.use(FakeChatModel.failingWith(new IllegalStateException("exercise upstream timeout")));

        mockMvc.perform(post("/api/exercises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topic\":\"集合框架\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("MODEL_CALL_FAILED"));

        assertThat(output.getOut())
                .contains("服务分支[Exercise模型调用失败]：条件=ChatClient调用抛出IllegalStateException")
                .contains("接口分支[生成请求失败] POST /api/exercises：条件=服务层抛出ModelCallException")
                .contains("异常分支[下游模型调用失败]：条件=捕获ModelCallException，返回502")
                .contains("exercise upstream timeout");
    }
}
