package dev.javaagentstudy.stage01.explain;

import dev.javaagentstudy.stage01.web.ApiError;
import dev.javaagentstudy.stage01.web.RequestLogSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;

/**
 * 概念讲解的 HTTP 入口：POST /api/explanations（同步）与 POST /api/explanations/stream（SSE 流式）。
 *
 * <p>Controller 只负责接收请求、返回响应，具体逻辑交给 Service。
 * 本机回环端口 CORS 策略由 {@link dev.javaagentstudy.stage01.web.LocalCorsConfig} 统一配置。
 */
@Tag(name = "Concept Explanation", description = "面向初级 Java Learner 的概念讲解接口（同步与 SSE 流式）")
@RestController
@RequestMapping("/api/explanations")
public class ConceptExplanationController {

    private static final Logger log = LoggerFactory.getLogger(ConceptExplanationController.class);

    private final ConceptExplanationService service;

    public ConceptExplanationController(ConceptExplanationService service) {
        this.service = service;
    }

    // @Valid 会在进入方法前校验请求体；校验失败时直接抛异常，Service 和模型都不会被调用。
    @Operation(summary = "同步生成概念讲解",
            description = "围绕指定 Topic（可选附带 focus 关注重点）生成完整概念讲解文本。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "讲解生成成功",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ExplanationResponse.class))),
            @ApiResponse(responseCode = "400", description = "参数校验失败或请求体 JSON 非法",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "502", description = "下游模型调用失败",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ExplanationResponse explain(@Valid @RequestBody ExplanationRequest request) {
        long startNanos = System.nanoTime();
        int topicLength = RequestLogSupport.lengthOf(request.topic());
        boolean hasFocus = RequestLogSupport.hasText(request.focus());
        log.info("接口入口[同步讲解] POST /api/explanations：mode=sync, topicLength={}, hasFocus={}",
                topicLength, hasFocus);
        try {
            ExplanationResponse response = service.explain(request);
            int responseChars = RequestLogSupport.lengthOf(response.explanation());
            log.info("接口分支[同步成功返回] POST /api/explanations：条件=服务返回讲解结果，responseChars={}, elapsedMs={}",
                    responseChars, RequestLogSupport.elapsedMs(startNanos));
            return response;
        } catch (RuntimeException ex) {
            log.warn("接口分支[同步请求失败] POST /api/explanations：条件=服务层抛出{}，elapsedMs={}",
                    ex.getClass().getSimpleName(), RequestLogSupport.elapsedMs(startNanos), ex);
            throw ex;
        }
    }

    // SSE 流式讲解：produces 指定 text/event-stream，Flux<String> 的每个元素会作为一个 data 事件推送。
    // 入参校验与同步接口一致：校验失败时在进入方法前抛异常，不会触发模型流式调用。
    @Operation(summary = "SSE 流式生成概念讲解",
            description = "以 text/event-stream 逐段推送概念讲解文本；入参校验失败仍返回 JSON 错误体。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SSE 事件流建立成功，按序推送讲解分片",
                    content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
                            schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "400", description = "参数校验失败或请求体 JSON 非法",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> explainStream(@Valid @RequestBody ExplanationRequest request) {
        log.info("接口入口[流式讲解] POST /api/explanations/stream：mode=stream, topicLength={}, hasFocus={}",
                RequestLogSupport.lengthOf(request.topic()), RequestLogSupport.hasText(request.focus()));
        // 流式响应在返回 Flux 后才由容器异步订阅；后续分片、完成、取消或异常日志在 Service 的 Flux 生命周期中记录。
        Flux<String> stream = service.explainStream(request);
        log.info("接口分支[流式响应建立] POST /api/explanations/stream：条件=已构建Flux响应流，等待容器订阅推送");
        return stream;
    }
}
