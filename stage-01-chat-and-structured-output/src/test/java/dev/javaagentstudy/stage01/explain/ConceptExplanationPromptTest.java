package dev.javaagentstudy.stage01.explain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.javaagentstudy.common.testing.FakeChatModel;
import dev.javaagentstudy.common.testing.SwitchableChatModel;
import dev.javaagentstudy.stage01.Stage01Application;
import dev.javaagentstudy.stage01.Stage01TestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 讲解提示词的离线测试（接缝 1：HTTP 入口）。
 *
 * <p>通过 HTTP 发送请求，再检查假模型“收到”的系统提示词，
 * 确认提示词明确面向初级 Java Learner，并要求涵盖三部分内容。
 */
@SpringBootTest(classes = {Stage01Application.class, Stage01TestConfig.class})
@AutoConfigureMockMvc
class ConceptExplanationPromptTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SwitchableChatModel fakeChatModel;

    @BeforeEach
    void setUp() {
        fakeChatModel.use(FakeChatModel.replyingWith("ok"));
    }

    @Test
    void systemPromptTargetsJuniorLearnerAndCoversThreeParts() throws Exception {
        mockMvc.perform(post("/api/explanations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topic\":\"集合框架\"}"))
                .andExpect(status().isOk());

        // 取出发给模型的系统消息，检查关键要求是否都在其中
        String systemText = fakeChatModel.recordedPrompts().get(0).getInstructions().stream()
                .filter(m -> m.getMessageType() == MessageType.SYSTEM)
                .map(Message::getText)
                .findFirst()
                .orElse("");
        assertThat(systemText)
                .contains("初级")
                .contains("核心原理")
                .contains("典型")
                .contains("误区");
    }
}
