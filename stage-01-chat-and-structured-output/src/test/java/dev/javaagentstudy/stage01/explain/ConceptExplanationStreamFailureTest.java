package dev.javaagentstudy.stage01.explain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

import java.nio.charset.StandardCharsets;

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
import org.springframework.test.web.servlet.MvcResult;

/**
 * SSE 流式讲解“流中途出错”的离线测试（接缝 1：HTTP 入口）。
 *
 * <p>假模型先回放两段分片，再以异常终止流。
 * 这里确认：已发出的分片不丢失、流确实以异常终止，并且上游异常原文不会泄露给调用方。
 */
@SpringBootTest(classes = {Stage01Application.class, ConceptExplanationStreamFailureTest.FailingStreamConfig.class})
@AutoConfigureMockMvc
class ConceptExplanationStreamFailureTest {

    /** 测试专用配置：流先输出两段分片，然后抛出带内部细节的异常。 */
    @TestConfiguration
    static class FailingStreamConfig {

        @Bean
        @Primary
        FakeChatModel fakeChatModel() {
            return FakeChatModel.streamingChunksThenFailing(
                    new IllegalStateException("upstream secret detail"), "集合", "框架");
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FakeChatModel fakeChatModel;

    @BeforeEach
    void resetRecordedPrompts() {
        fakeChatModel.clearRecordedPrompts();
    }

    @Test
    void midStreamFailureKeepsEmittedChunksAndDoesNotLeakUpstreamMessage() throws Exception {
        MvcResult asyncResult = mockMvc.perform(post("/api/explanations/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"topic\":\"集合框架\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        // 流中途出错时响应已开始写出，状态码无法再改成 502。
        // 先等待 asyncDispatch 结束（无论是正常返回还是抛出分发异常），再读取已写出的响应体，
        // 避免主线程在异步流线程尚未写出分片前就提前读取 MockHttpServletResponse 产生竞态。
        Throwable failure = null;
        try {
            mockMvc.perform(asyncDispatch(asyncResult));
        } catch (Exception ex) {
            failure = ex;
        }
        String body = asyncResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        Throwable asyncError = asyncResult.getAsyncResult() instanceof Throwable t ? t : null;

        // 验证：流以错误终止（异步结果或分发异常二者之一体现出错）
        assertThat(failure != null || asyncError != null || body.contains("error"))
                .as("流中途出错必须能被观察到")
                .isTrue();
        // 验证：出错前的分片已送达，且上游异常原文没有出现在响应体或对外错误信息中
        assertThat(body).contains("集合").contains("框架");
        assertThat(body).doesNotContain("upstream secret detail");
        assertThat(fakeChatModel.recordedPrompts()).hasSize(1);
    }
}
