package dev.javaagentstudy.stage01;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 默认 Profile 的真实启动回归测试。
 *
 * <p>不替换任何 {@link ChatModel} Bean，验证以默认 Profile 启动 {@link Stage01Application} 时，
 * 容器中仅存在单一 OpenAI {@link ChatModel}，且 {@link ChatClient.Builder} 可正常装配，
 * 不会再出现多 ChatModel 冲突。这里不会真的发起网络请求。
 */
@SpringBootTest(classes = Stage01Application.class)
class DefaultProfileStartupTest {

    @Autowired
    private List<ChatModel> chatModels;

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Test
    void defaultProfileProvidesSingleOpenAiChatModelAndBuildsChatClient() {
        assertThat(chatModels).hasSize(1);
        assertThat(chatModels.get(0).getClass().getSimpleName()).contains("OpenAi");
        assertThat(chatClientBuilder).isNotNull();
    }
}
