package dev.javaagentstudy.stage01.explain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.javaagentstudy.common.testing.FakeChatModel;
import dev.javaagentstudy.stage01.Stage01Application;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 概念讲解 HTTP 接口的离线集成测试（接缝 1：HTTP 入口）。
 *
 * <p>做法：启动完整的 Spring 上下文，通过 MockMvc 像真实客户端一样发送 HTTP 请求，
 * 只把最外层的 {@link ChatModel} 换成假模型。中间的 Controller、Service、ChatClient
 * 都是真实代码，这样测试既不花钱，又能覆盖真实的调用链路。
 */
@SpringBootTest(classes = {Stage01Application.class, ConceptExplanationApiTest.FakeModelConfig.class})
@AutoConfigureMockMvc
class ConceptExplanationApiTest {

    /** 测试专用配置：用假模型顶替真实的大模型。 */
    @TestConfiguration
    static class FakeModelConfig {

        @Bean
        @Primary
        FakeChatModel fakeChatModel() {
            return FakeChatModel.replyingWith("集合框架是 Java 用来存放一组对象的工具。");
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FakeChatModel fakeChatModel;

    @Test
    void returnsExplanationForValidTopic() throws Exception {
        // 执行：提交一个合法的 topic 和可选的 focus
        mockMvc.perform(post("/api/explanations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topic\":\"集合框架\",\"focus\":\"ArrayList 与 LinkedList 的区别\"}"))
                // 验证：返回 200，且 JSON 里包含 topic 和假模型预设的讲解文本
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topic").value("集合框架"))
                .andExpect(jsonPath("$.explanation").value("集合框架是 Java 用来存放一组对象的工具。"));

        // 验证：模型确实被调用了一次，且提示词包含 topic 与 focus
        // （提示词是否面向初级 Learner 的断言，留到下一个测试循环）
        assertThat(fakeChatModel.recordedPrompts()).hasSize(1);
        String sentPrompt = fakeChatModel.recordedPrompts().get(0).getContents();
        assertThat(sentPrompt).contains("集合框架").contains("ArrayList 与 LinkedList 的区别");
    }
}
