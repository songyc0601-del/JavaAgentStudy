package dev.javaagentstudy.stage01;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 阶段一的启动类。
 *
 * <p>@SpringBootApplication 会自动扫描本包及其子包下的 Controller、Service 等组件，
 * 并根据 pom.xml 里的依赖自动装配 Web 与 Spring AI。
 */
@SpringBootApplication
public class Stage01Application {

    public static void main(String[] args) {
        SpringApplication.run(Stage01Application.class, args);
    }
}
