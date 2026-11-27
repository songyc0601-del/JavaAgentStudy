package dev.javaagentstudy.stage01.explain;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
 * 验证静态调试页面可随应用正常提供访问，不依赖外部模型或网络。
 */
@SpringBootTest(classes = {Stage01Application.class, ConceptExplanationPageTest.FakeModelConfig.class})
@AutoConfigureMockMvc
class ConceptExplanationPageTest {

    @TestConfiguration
    static class FakeModelConfig {

        @Bean
        @Primary
        FakeChatModel fakeChatModel() {
            return FakeChatModel.replyingWith("占位回复");
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void servesVerificationHtmlPageAtIndex() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("/api/explanations")))
                .andExpect(content().string(containsString("/api/explanations/stream")));
    }
}
