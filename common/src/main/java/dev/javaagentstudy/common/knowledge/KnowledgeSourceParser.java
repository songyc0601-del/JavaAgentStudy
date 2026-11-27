package dev.javaagentstudy.common.knowledge;

/**
 * 负责将原始 Markdown / 纯文本内容解析为结构化的元信息与正文。
 *
 * <p>保持包内私有，作为 {@link KnowledgeSourceLoader} 的内部解析协作类，
 * 对外仍只暴露统一的 {@link KnowledgeSourceLoader} 公共接口。
 */
class KnowledgeSourceParser {

    /**
     * 解析单条原始文本，提取可选 frontmatter 中的 {@code id}、{@code title}、{@code topic} 与正文。
     *
     * @param rawText UTF-8 原始文本
     * @return 解析后的结构化结果（未提供某项元信息时对应字段为 {@code null}）
     */
    ParsedKnowledgeSource parse(String rawText) {
        String normalized = stripBom(rawText).replace("\r\n", "\n").replace("\r", "\n");
        if (!normalized.startsWith("---\n")) {
            return new ParsedKnowledgeSource(null, null, null, normalized.trim());
        }

        // 查找闭合分隔符 "\n---\n"。起点取 3（开头 "---" 之后的换行位置）而不是 4：
        // 这样空 frontmatter（"---\n---\n正文"）中紧跟的第二个 "---" 也能匹配到，
        // 否则会找不到闭合标记，导致整段被误当作正文。
        int closingIndex = normalized.indexOf("\n---\n", 3);
        int bodyStart;
        if (closingIndex >= 0) {
            bodyStart = closingIndex + "\n---\n".length();
        } else if (normalized.endsWith("\n---")) {
            // 文件以闭合分隔符结尾且其后没有换行：只有头部、没有正文的情况，bodyStart 指向末尾使正文为空。
            closingIndex = normalized.length() - "\n---".length();
            bodyStart = normalized.length();
        } else {
            return new ParsedKnowledgeSource(null, null, null, normalized.trim());
        }

        String id = null;
        String title = null;
        String topic = null;
        // 头部区间为 [4, closingIndex)。空 frontmatter 时 closingIndex == 3，
        // 此时 substring(4, 3) 会越界抛异常，所以用 closingIndex > 4 保护，空头部直接视为无元信息。
        String headerBlock = closingIndex > 4 ? normalized.substring(4, closingIndex) : "";
        for (String line : headerBlock.split("\n")) {
            int separatorIndex = line.indexOf(':');
            if (separatorIndex <= 0) {
                continue;
            }
            String key = line.substring(0, separatorIndex).trim();
            String value = stripSurroundingQuotes(line.substring(separatorIndex + 1).trim());
            if (value.isEmpty()) {
                continue;
            }
            switch (key) {
                case "id" -> id = value;
                case "title" -> title = value;
                case "topic" -> topic = value;
                default -> {
                    // 忽略非核心扩展字段，保持宽容解析。
                }
            }
        }
        String body = normalized.substring(bodyStart).trim();
        return new ParsedKnowledgeSource(id, title, topic, body);
    }

    /**
     * 提取 Markdown 正文中的首个一级标题（{@code # 标题}）。
     *
     * @param body 正文内容
     * @return 首个非空一级标题；不存在时返回 {@code null}
     */
    String extractFirstMarkdownHeading(String body) {
        for (String line : body.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("# ")) {
                String heading = trimmed.substring(2).trim();
                if (!heading.isBlank()) {
                    return heading;
                }
            }
        }
        return null;
    }

    private String stripBom(String text) {
        return text.startsWith("\uFEFF") ? text.substring(1) : text;
    }

    private String stripSurroundingQuotes(String value) {
        if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1).trim();
        }
        return value;
    }

    record ParsedKnowledgeSource(String id, String title, String topic, String body) {
    }
}
