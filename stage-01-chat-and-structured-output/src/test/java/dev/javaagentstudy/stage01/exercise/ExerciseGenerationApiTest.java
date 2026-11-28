package dev.javaagentstudy.stage01.exercise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.javaagentstudy.common.testing.FakeChatModel;
import dev.javaagentstudy.common.testing.SwitchableChatModel;
import dev.javaagentstudy.stage01.Stage01Application;
import dev.javaagentstudy.stage01.Stage01TestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 结构化 Exercise 生成接口的离线集成测试（接缝：HTTP 入口）。
 *
 * <p>启动完整 Spring 上下文，只把最外层 {@link ChatModel} 换成假模型，
 * 其余 Controller、Service、ChatClient 与结构化输出转换均为真实代码。
 */
@SpringBootTest(classes = {Stage01Application.class, Stage01TestConfig.class})
@AutoConfigureMockMvc
class ExerciseGenerationApiTest {

    /** 假模型预设一段合法的 Exercise JSON，模拟模型严格遵循输出格式。 */
    private static final String VALID_EXERCISE_JSON = """
            {
              "title": "ArrayList 与 LinkedList 的选择",
              "topic": {"name": "集合框架"},
              "difficulty": "BEGINNER",
              "description": "说明在频繁随机访问与频繁头部插入两种场景下应选哪种 List。",
              "hints": ["回忆两者的底层数据结构", "比较随机访问与插入的时间复杂度"],
              "referenceAnswerPoints": ["ArrayList 基于数组，随机访问 O(1)", "LinkedList 基于链表，头部插入 O(1)"]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SwitchableChatModel fakeChatModel;

    @BeforeEach
    void resetRecordedPrompts() {
        // 每个测试装入干净的假模型，避免复用上下文时跨用例互相影响。
        fakeChatModel.use(FakeChatModel.replyingWith(VALID_EXERCISE_JSON));
    }

    @Test
    void generatesStronglyTypedExerciseForValidTopic() throws Exception {
        mockMvc.perform(post("/api/exercises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topic\":\"集合框架\",\"difficulty\":\"BEGINNER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("ArrayList 与 LinkedList 的选择"))
                .andExpect(jsonPath("$.topic.name").value("集合框架"))
                .andExpect(jsonPath("$.difficulty").value("BEGINNER"))
                .andExpect(jsonPath("$.description").isNotEmpty())
                .andExpect(jsonPath("$.hints.length()").value(2))
                .andExpect(jsonPath("$.referenceAnswerPoints.length()").value(2));

        // 模型恰好被调用一次，且提示词包含 topic。
        assertThat(fakeChatModel.recordedPrompts()).hasSize(1);
        assertThat(fakeChatModel.recordedPrompts().get(0).getContents()).contains("集合框架");
    }

    @Test
    void usesBeginnerDifficultyWhenNotSpecified() throws Exception {
        mockMvc.perform(post("/api/exercises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topic\":\"集合框架\"}"))
                .andExpect(status().isOk());

        // 断言“难度：BEGINNER”这句提示词，避免与 getFormat() 注入的 Schema 里的枚举取值混淆。
        assertThat(fakeChatModel.recordedPrompts()).hasSize(1);
        assertThat(fakeChatModel.recordedPrompts().get(0).getContents()).contains("难度：BEGINNER");
    }

    @Test
    void rejectsInvalidInputWithoutCallingModel() throws Exception {
        // 空白 topic、缺失 topic、非法难度枚举都应返回 400，且不触发模型调用。
        for (String body : new String[] {
                "{\"topic\":\"  \"}",
                "{}",
                "{\"topic\":\"集合框架\",\"difficulty\":\"EXPERT\"}" }) {
            mockMvc.perform(post("/api/exercises")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    // 同时断言结构化错误体，而不只是状态码。
                    .andExpect(jsonPath("$.error").isNotEmpty());
        }
        assertThat(fakeChatModel.recordedPrompts()).isEmpty();
    }

    @Test
    void passesExplicitDifficultyAndFocusToModel() throws Exception {
        mockMvc.perform(post("/api/exercises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topic\":\"集合框架\",\"difficulty\":\"ADVANCED\",\"focus\":\"HashMap 扩容机制\"}"))
                .andExpect(status().isOk());

        // 显式难度与关注重点都应进入发给模型的提示词。
        assertThat(fakeChatModel.recordedPrompts()).hasSize(1);
        String sentPrompt = fakeChatModel.recordedPrompts().get(0).getContents();
        assertThat(sentPrompt).contains("难度：ADVANCED").contains("HashMap 扩容机制");
        assertThat(sentPrompt).doesNotContain("难度：BEGINNER");
    }
}
