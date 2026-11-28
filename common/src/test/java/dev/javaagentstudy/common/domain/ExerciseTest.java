package dev.javaagentstudy.common.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Exercise 领域记录的构造契约测试：验证必填字段非空白、列表非空且不可变。
 */
class ExerciseTest {

    private static final Topic TOPIC = new Topic("集合框架");

    @Test
    void createsImmutableExerciseForValidFields() {
        List<String> mutableHints = new ArrayList<>(List.of("回忆底层数据结构"));
        List<String> mutablePoints = new ArrayList<>(List.of("ArrayList 基于数组"));

        Exercise exercise = new Exercise(
                "对比 List 实现", TOPIC, Difficulty.BEGINNER, "比较 ArrayList 与 LinkedList",
                mutableHints, mutablePoints);

        // 修改外部原始列表不影响已构造的 Exercise。
        mutableHints.add("不应被看到");
        mutablePoints.add("不应被看到");

        assertThat(exercise.hints()).containsExactly("回忆底层数据结构");
        assertThat(exercise.referenceAnswerPoints()).containsExactly("ArrayList 基于数组");
    }

    @Test
    void rejectsBlankTitleOrDescription() {
        assertThatThrownBy(() -> new Exercise(
                "  ", TOPIC, Difficulty.BEGINNER, "题干", List.of("提示"), List.of("要点")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Exercise(
                "标题", TOPIC, Difficulty.BEGINNER, "", List.of("提示"), List.of("要点")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullTopicOrDifficulty() {
        assertThatThrownBy(() -> new Exercise(
                "标题", null, Difficulty.BEGINNER, "题干", List.of("提示"), List.of("要点")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Exercise(
                "标题", TOPIC, null, "题干", List.of("提示"), List.of("要点")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsEmptyOrBlankListItems() {
        assertThatThrownBy(() -> new Exercise(
                "标题", TOPIC, Difficulty.BEGINNER, "题干", List.of(), List.of("要点")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Exercise(
                "标题", TOPIC, Difficulty.BEGINNER, "题干", List.of("提示"), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Exercise(
                "标题", TOPIC, Difficulty.BEGINNER, "题干", List.of(" "), List.of("要点")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Exercise(
                "标题", TOPIC, Difficulty.BEGINNER, "题干", List.of("提示"), List.of(" ")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
