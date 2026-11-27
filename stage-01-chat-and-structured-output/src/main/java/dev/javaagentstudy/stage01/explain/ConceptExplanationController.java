package dev.javaagentstudy.stage01.explain;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 概念讲解的 HTTP 入口：POST /api/explanations。
 *
 * <p>Controller 只负责接收请求、返回响应，具体逻辑交给 Service。
 */
@RestController
@RequestMapping("/api/explanations")
public class ConceptExplanationController {

    private final ConceptExplanationService service;

    public ConceptExplanationController(ConceptExplanationService service) {
        this.service = service;
    }

    // @Valid 会在进入方法前校验请求体；校验失败时直接抛异常，Service 和模型都不会被调用。
    @PostMapping
    public ExplanationResponse explain(@Valid @RequestBody ExplanationRequest request) {
        return service.explain(request);
    }
}
