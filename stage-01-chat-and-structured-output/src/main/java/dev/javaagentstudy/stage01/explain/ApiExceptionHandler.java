package dev.javaagentstudy.stage01.explain;

import java.util.List;

import dev.javaagentstudy.stage01.exercise.ExerciseFormatException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 集中处理接口抛出的异常，把它们转换成结构化的 JSON 错误响应。
 *
 * <p>@RestControllerAdvice 对所有 Controller 生效。
 * 这里只处理“已知类型”的异常，不捕获宽泛的 RuntimeException，
 * 这样我们自己代码的 bug 不会被伪装成模型故障。
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /** 入参校验失败：返回 400，并列出每个字段的问题。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .toList();
        log.warn("异常分支[参数校验失败]：条件=触发MethodArgumentNotValidException，details={}", details, ex);
        ApiError body = new ApiError("VALIDATION_FAILED", "Request validation failed", details);
        // 显式指定 JSON：SSE 客户端会带 Accept: text/event-stream，
        // 不指定时 ApiError 无法按该类型序列化，会返回空响应体。
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(body);
    }

    /** 请求体为空或不是合法 JSON：这是调用方的错误，返回 400，不会调用模型。 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleMalformedRequest(HttpMessageNotReadableException ex) {
        log.warn("异常分支[请求体格式错误]：条件=触发HttpMessageNotReadableException，返回400", ex);
        ApiError body = new ApiError("MALFORMED_REQUEST",
                "Request body is missing or is not valid JSON", List.of());
        // 同上：流式端点的客户端带 Accept: text/event-stream，这里同样固定为 JSON，避免空响应体。
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(body);
    }

    /**
     * 下游模型调用失败（网络、额度、服务故障等）：返回 502，表示问题出在上游依赖。
     *
     * <p>注意：响应里只放固定文案，不拼接上游异常的原文，
     * 因为上游错误可能包含地址、模型名、额度等内部细节。
     * 详细原因只写入服务端日志，供开发者排查。
     */
    @ExceptionHandler(ModelCallException.class)
    public ResponseEntity<ApiError> handleModelFailure(ModelCallException ex) {
        log.error("异常分支[下游模型调用失败]：条件=捕获ModelCallException，返回502", ex);
        ApiError body = new ApiError("MODEL_CALL_FAILED",
                "The language model is currently unavailable, please try again later", List.of());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }

    /** 模型输出经校验重试后仍不是合法 Exercise：同样属于上游问题，返回 502，但用独立错误码区分。 */
    @ExceptionHandler(ExerciseFormatException.class)
    public ResponseEntity<ApiError> handleExerciseFormat(ExerciseFormatException ex) {
        // 不附带异常对象：其 cause 的 message 可能含模型输出片段；详细类型已在 Service 层记录。
        log.error("异常分支[Exercise格式不合法]：条件=捕获ExerciseFormatException，返回502");
        ApiError body = new ApiError("EXERCISE_GENERATION_FAILED",
                "The model did not return a valid exercise, please try again later", List.of());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }
}
