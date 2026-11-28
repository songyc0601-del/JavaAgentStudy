package dev.javaagentstudy.stage01.exercise;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.javaagentstudy.common.testing.FakeChatModel;
import dev.javaagentstudy.common.testing.SwitchableChatModel;
import dev.javaagentstudy.stage01.Stage01Application;
import dev.javaagentstudy.stage01.Stage01TestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 下游模型抛出异常时的离线集成测试（接缝：HTTP 入口）。
 */
@SpringBootTest(classes = {Stage01Application.class, Stage01TestConfig.class})
@AutoConfigureMockMvc
class ExerciseModelFailureTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SwitchableChatModel fakeChatModel;

    @BeforeEach
    void setUp() {
        fakeChatModel.use(FakeChatModel.failingWith(new IllegalStateException("model down")));
    }

    @Test
    void returnsModelCallFailedWhenModelThrows() throws Exception {
        mockMvc.perform(post("/api/exercises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topic\":\"集合框架\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("MODEL_CALL_FAILED"));
    }
}
