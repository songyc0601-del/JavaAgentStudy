package dev.javaagentstudy.stage01.exercise;

import dev.javaagentstudy.common.domain.Exercise;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exercise 生成的 HTTP 入口：POST /api/exercises。
 */
@RestController
@RequestMapping("/api/exercises")
public class ExerciseController {

    private static final Logger log = LoggerFactory.getLogger(ExerciseController.class);

    private final ExerciseService service;

    public ExerciseController(ExerciseService service) {
        this.service = service;
    }

    // @Valid 校验失败会在进入方法前抛异常，Service 与模型都不会被调用。
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
