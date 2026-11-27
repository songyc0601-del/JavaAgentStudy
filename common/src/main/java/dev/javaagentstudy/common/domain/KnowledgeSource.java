package dev.javaagentstudy.common.domain;

/**
 * Knowledge Source：供 Study Agent 检索与引用、用于支撑概念讲解与出题的原始学习资料。
 *
 * <p>这是一个不可变的 record，确保每一条知识源在创建时都具备有效的标识、标题、
 * 归属 {@link Topic} 与正文内容。
 *
 * @param id      知识源唯一标识，不能为 null 或空白
 * @param title   知识源标题，不能为 null 或空白
 * @param topic   知识点归属 {@link Topic}，不能为 null
 * @param content 知识源正文内容，不能为 null 或空白
 */
public record KnowledgeSource(
        String id,
        String title,
        Topic topic,
        String content) {

    public KnowledgeSource {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("KnowledgeSource id must not be blank");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("KnowledgeSource title must not be blank");
        }
        if (topic == null) {
            throw new IllegalArgumentException("KnowledgeSource topic must not be null");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("KnowledgeSource content must not be blank");
        }
    }
}
