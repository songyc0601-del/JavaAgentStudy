package dev.javaagentstudy.stage01.explain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
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
import org.springframework.test.web.servlet.MvcResult;

import reactor.core.publisher.Flux;

/**
 * 验证同步与流式两个概念讲解接口在成功、校验失败、异常与取消场景下的简体中文分支日志，
 * 并确认提示词与模型输出正文不会泄露到日志中。
 */
@SpringBootTest(classes = {Stage01Application.class, ConceptExplanationLoggingTest.SwitchableModelConfig.class})
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class ConceptExplanationLoggingTest {

    /** 可按测试方法切换底层 FakeChatModel 的测试替身，避免为不同场景重复拉起 Spring 上下文。 */
    static class SwitchableChatModel implements ChatModel {

        private final AtomicReference<FakeChatModel> delegate =
                new AtomicReference<>(FakeChatModel.replyingWith("ok"));

        void use(FakeChatModel model) {
            delegate.set(model);
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

    @Autowired
    private ConceptExplanationService service;

    @Test
    void logsSyncBranchesWithoutLeakingPromptOrModelOutput(CapturedOutput output) throws Exception {
        String reply = "SECRET_MODEL_OUTPUT_集合框架用于组织对象";
        chatModel.use(FakeChatModel.replyingWith(reply));

        mockMvc.perform(post("/api/explanations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topic\":\"SECRET_TOPIC_集合框架\",\"focus\":\"SECRET_FOCUS_List与Set\"}"))
                .andExpect(status().isOk());

        assertThat(output.getOut())
                .contains("接口入口[同步讲解] POST /api/explanations：mode=sync, topicLength=17, hasFocus=true")
                .contains("服务入口[同步讲解] ConceptExplanationService.explain：mode=sync, topicLength=17, hasFocus=true")
                .contains("提示词分支[附带focus]：条件=focus非空白，topicLength=17, focusLength=21, promptChars=")
                .contains("服务分支[同步模型调用成功]：条件=模型返回非空内容，promptChars=")
                .contains("接口分支[同步成功返回] POST /api/explanations：条件=服务返回讲解结果，responseChars=" + reply.length())
                .doesNotContain("SECRET_TOPIC_集合框架")
                .doesNotContain("SECRET_FOCUS_List与Set")
                .doesNotContain(reply)
                .doesNotContain("请讲解 Java 主题");
    }

    @Test
    void logsValidationAndMalformedRequestBranches(CapturedOutput output) throws Exception {
        mockMvc.perform(post("/api/explanations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topic\":\"  \"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/explanations/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{bad json"))
                .andExpect(status().isBadRequest());

        assertThat(output.getOut())
                .contains("异常分支[参数校验失败]：条件=触发MethodArgumentNotValidException，details=[topic: topic must not be blank]")
                .contains("异常分支[请求体格式错误]：条件=触发HttpMessageNotReadableException，返回400");
    }

    @Test
    void logsSyncModelFailureWithCause(CapturedOutput output) throws Exception {
        chatModel.use(FakeChatModel.failingWith(new IllegalStateException("upstream timeout")));

        mockMvc.perform(post("/api/explanations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topic\":\"并发基础\"}"))
                .andExpect(status().isBadGateway());

        assertThat(output.getOut())
                .contains("提示词分支[仅基础topic]：条件=focus为null或空白，topicLength=4, promptChars=")
                .contains("服务分支[同步模型调用失败]：条件=ChatClient调用抛出IllegalStateException")
                .contains("接口分支[同步请求失败] POST /api/explanations：条件=服务层抛出ModelCallException")
                .contains("异常分支[下游模型调用失败]：条件=捕获ModelCallException，返回502")
                .contains("upstream timeout");
    }

    @Test
    void logsStreamBranchesAndChunkMetricsWithoutChunkText(CapturedOutput output) throws Exception {
        chatModel.use(FakeChatModel.streamingChunks("CHUNK_ONE", "CHUNK_TWO", "CHUNK_THREE"));

        MvcResult started = mockMvc.perform(post("/api/explanations/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"topic\":\"集合框架\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(started))
                .andExpect(status().isOk());

        assertThat(output.getOut())
                .contains("接口入口[流式讲解] POST /api/explanations/stream：mode=stream, topicLength=4, hasFocus=false")
                .contains("服务入口[流式讲解] ConceptExplanationService.explainStream：mode=stream, topicLength=4, hasFocus=false")
                .contains("提示词分支[仅基础topic]：条件=focus为null或空白，topicLength=4, promptChars=")
                .contains("接口分支[流式响应建立] POST /api/explanations/stream：条件=已构建Flux响应流，等待容器订阅推送")
                .contains("流式分支[订阅开始]：条件=收到下游订阅信号，promptChars=")
                .contains("流式分支[分片产出]：chunkIndex=1, chunkChars=9, totalChars=9")
                .contains("流式分支[流正常完成]：条件=收到complete信号，chunks=3, totalChars=29, elapsedMs=")
                .doesNotContain("CHUNK_ONE")
                .doesNotContain("CHUNK_TWO")
                .doesNotContain("CHUNK_THREE");
    }

    @Test
    void logsMidStreamFailureWithCause(CapturedOutput output) throws Exception {
        chatModel.use(FakeChatModel.streamingChunksThenFailing(
                new IllegalStateException("stream broken"), "集合", "框架"));

        MvcResult started = mockMvc.perform(post("/api/explanations/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"topic\":\"集合框架\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        try {
            mockMvc.perform(asyncDispatch(started));
        } catch (Exception ignored) {
            // 流中途出错时 asyncDispatch 可能抛出异常，这里关注服务端日志是否记录完整故障上下文。
        }

        assertThat(output.getOut())
                .contains("流式分支[流异常终止]：条件=收到error信号，chunks=2, totalChars=4, elapsedMs=")
                .contains("ModelCallException")
                .contains("stream broken");
    }

    @Test
    void logsStreamCancellation(CapturedOutput output) {
        chatModel.use(FakeChatModel.streamingChunks("第一段", "第二段", "第三段"));

        String firstChunk = service.explainStream(new ExplanationRequest("JVM 内存", null))
                .take(1)
                .blockLast();

        assertThat(firstChunk).isEqualTo("第一段");
        assertThat(output.getOut())
                .contains("流式分支[流被取消]：条件=终态前收到cancel信号，chunks=1, totalChars=3, elapsedMs=");
    }
}
