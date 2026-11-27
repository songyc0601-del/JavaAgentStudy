package dev.javaagentstudy.stage01.explain;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;

/**
 * 概念讲解的 HTTP 入口：POST /api/explanations（同步）与 POST /api/explanations/stream（SSE 流式）。
 *
 * <p>Controller 只负责接收请求、返回响应，具体逻辑交给 Service。
 */
// 仅放行本机回环端口跨源，方便直接用 IntelliJ 内置静态预览（如 localhost:63342）打开调试页联调。
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"})
@RestController
@RequestMapping("/api/explanations")
public class ConceptExplanationController {

    private static final Logger log = LoggerFactory.getLogger(ConceptExplanationController.class);

    private final ConceptExplanationService service;

    public ConceptExplanationController(ConceptExplanationService service) {
        this.service = service;
    }

    // @Valid 会在进入方法前校验请求体；校验失败时直接抛异常，Service 和模型都不会被调用。
    @PostMapping
    public ExplanationResponse explain(@Valid @RequestBody ExplanationRequest request) {
        long startNanos = System.nanoTime();
        int topicLength = ExplainLogSupport.topicLength(request);
        boolean hasFocus = ExplainLogSupport.hasFocus(request);
        log.info("接口入口[同步讲解] POST /api/explanations：mode=sync, topicLength={}, hasFocus={}",
                topicLength, hasFocus);
        try {
            ExplanationResponse response = service.explain(request);
            int responseChars = response.explanation() != null ? response.explanation().length() : 0;
            log.info("接口分支[同步成功返回] POST /api/explanations：条件=服务返回讲解结果，responseChars={}, elapsedMs={}",
                    responseChars, ExplainLogSupport.elapsedMs(startNanos));
            return response;
        } catch (RuntimeException ex) {
            log.warn("接口分支[同步请求失败] POST /api/explanations：条件=服务层抛出{}，elapsedMs={}",
                    ex.getClass().getSimpleName(), ExplainLogSupport.elapsedMs(startNanos), ex);
            throw ex;
        }
    }

    // SSE 流式讲解：produces 指定 text/event-stream，Flux<String> 的每个元素会作为一个 data 事件推送。
    // 入参校验与同步接口一致：校验失败时在进入方法前抛异常，不会触发模型流式调用。
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> explainStream(@Valid @RequestBody ExplanationRequest request) {
        log.info("接口入口[流式讲解] POST /api/explanations/stream：mode=stream, topicLength={}, hasFocus={}",
                ExplainLogSupport.topicLength(request), ExplainLogSupport.hasFocus(request));
        // 流式响应在返回 Flux 后才由容器异步订阅；后续分片、完成、取消或异常日志在 Service 的 Flux 生命周期中记录。
        Flux<String> stream = service.explainStream(request);
        log.info("接口分支[流式响应建立] POST /api/explanations/stream：条件=已构建Flux响应流，等待容器订阅推送");
        return stream;
    }
}
