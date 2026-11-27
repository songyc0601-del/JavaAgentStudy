package dev.javaagentstudy.stage01;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * ollama Profile 的配置测试。
 *
 * <p>pom 里同时引入了 openai 和 ollama 两个 starter，如果配置不对，
 * 容器里会出现两个 ChatModel，ChatClient.Builder 就无法确定用哪个。
 * 这个测试确认：启用 ollama Profile 后，容器里只有一个 ChatModel，
 * 并且它是 Ollama 的实现。这里不替换任何 Bean，也不会真的访问网络。
 */
@SpringBootTest(classes = Stage01Application.class)
@ActiveProfiles("ollama")
class OllamaProfileTest {

    @Autowired
    private List<ChatModel> chatModels;

    @Test
    void onlyOneChatModelIsAvailableAndItIsOllama() {
        assertThat(chatModels).hasSize(1);
        assertThat(chatModels.get(0).getClass().getSimpleName()).contains("Ollama");
    }
}
