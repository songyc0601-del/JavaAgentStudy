package dev.javaagentstudy.stage01.explain;

/**
 * 表示“调用下游大模型失败”的专用异常。
 *
 * <p>为什么要单独定义一个异常？
 * 如果异常处理器直接捕获 RuntimeException，我们自己代码里的 bug（例如空指针）
 * 也会被误报成“模型故障”。用专用异常后，只有真正的模型调用失败才会走到 502 分支。
 */
public class ModelCallException extends RuntimeException {

    /**
     * @param cause 原始异常，只用于服务端日志排查，不会返回给调用方
     */
    public ModelCallException(Throwable cause) {
        super("Language model call failed", cause);
    }
}
