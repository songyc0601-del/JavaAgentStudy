package dev.javaagentstudy.stage01.exercise;

import dev.javaagentstudy.common.domain.Exercise;
import dev.javaagentstudy.stage01.explain.ApiError;
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

/**
 * Exercise 生成的 HTTP 入口：POST /api/exercises。
 */
@Tag(name = "Exercise", description = "围绕指定 Topic 生成强类型结构化 Exercise 的接口")
@RestController
@RequestMapping("/api/exercises")
public class ExerciseController {

    private static final Logger log = LoggerFactory.getLogger(ExerciseController.class);

    private final ExerciseService service;

    public ExerciseController(ExerciseService service) {
        this.service = service;
    }

    // @Valid 校验失败会在进入方法前抛异常，Service 与模型都不会被调用。
    @Operation(summary = "生成结构化 Exercise",
            description = "按 Topic、可选 Difficulty（默认 BEGINNER）与可选 focus 生成带提示与参考要点的 Exercise。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Exercise 生成成功",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Exercise.class))),
            @ApiResponse(responseCode = "400", description = "参数校验失败、Difficulty 非法或请求体 JSON 非法",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "502", description = "下游模型调用失败或重试后仍无法产出合法 Exercise",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public Exercise generate(@Valid @RequestBody ExerciseRequest request) {
        log.info("接口入口[生成Exercise] POST /api/exercises：topicLength={}, hasDifficulty={}, hasFocus={}",
                request.topic().length(), request.difficulty() != null,
                request.focus() != null && !request.focus().isBlank());
        Exercise exercise = service.generate(request);
        log.info("接口分支[生成成功返回] POST /api/exercises：条件=服务返回Exercise，hints={}, points={}",
                exercise.hints().size(), exercise.referenceAnswerPoints().size());
        return exercise;
    }
}
