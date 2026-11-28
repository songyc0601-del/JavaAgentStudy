package dev.javaagentstudy.common.knowledge;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import dev.javaagentstudy.common.domain.KnowledgeSource;
import dev.javaagentstudy.common.domain.Topic;
import dev.javaagentstudy.common.knowledge.KnowledgeSourceParser.ParsedKnowledgeSource;

/**
 * Knowledge Source 加载器。
 *
 * <p>默认加载 {@code common} 模块内置的初级 Java 核心 {@link Topic} Markdown 资料，
 * 并支持在不修改内置资源的前提下，从可选外部目录增量合并个人 {@code .md} / {@code .txt} 学习笔记。
 */
public class KnowledgeSourceLoader {

    /** 内置默认知识源在 classpath 下的固定路径列表，保持加载顺序稳定。 */
    private static final List<String> BUILTIN_RESOURCE_PATHS = List.of(
            "knowledge/collections-hashmap.md",
            "knowledge/jvm-memory-basics.md",
            "knowledge/concurrency-basics.md",
            "knowledge/spring-ioc-aop-pitfalls.md");

    /**
     * 使用 JDK 自带的 {@link System.Logger}，避免为 common 模块引入额外的日志依赖；
     * 实际输出由运行环境绑定的日志实现（如 SLF4J）决定。
     */
    private static final System.Logger LOGGER = System.getLogger(KnowledgeSourceLoader.class.getName());

    private final KnowledgeSourceParser parser = new KnowledgeSourceParser();

    /**
     * 加载内置默认 {@link KnowledgeSource} 集合。
     *
     * @return 不可变的内置知识源列表
     */
    public List<KnowledgeSource> load() {
        return load(null);
    }

    /**
     * 加载内置默认 {@link KnowledgeSource}，并将可选外部目录中的 {@code .md} / {@code .txt} 笔记增量合并返回。
     *
     * <p>当 {@code externalDirectory} 为 {@code null}、路径不存在、非目录或为空目录时，
     * 安全回退为内置默认集合而不抛出异常；扫描外部目录时自动忽略隐藏项、子目录、
     * 非目标扩展名与纯空白笔记。
     *
     * @param externalDirectory 可选的外部个人笔记目录，可为 {@code null}
     * @return 不可变的知识源合并列表（内置默认集合在前，外部笔记按名称排序追加在后）
     */
    public List<KnowledgeSource> load(Path externalDirectory) {
        List<KnowledgeSource> sources = new ArrayList<>(loadBuiltInSources());
        if (externalDirectory == null
                || !Files.exists(externalDirectory)
                || !Files.isDirectory(externalDirectory)) {
            return List.copyOf(sources);
        }

        Set<String> usedIds = new HashSet<>();
        for (KnowledgeSource source : sources) {
            usedIds.add(source.id());
        }
        sources.addAll(loadExternalSources(externalDirectory, usedIds));
        return List.copyOf(sources);
    }

    private List<KnowledgeSource> loadBuiltInSources() {
        List<KnowledgeSource> sources = new ArrayList<>(BUILTIN_RESOURCE_PATHS.size());
        for (String resourcePath : BUILTIN_RESOURCE_PATHS) {
            sources.add(loadBuiltInResource(resourcePath));
        }
        return sources;
    }

    private KnowledgeSource loadBuiltInResource(String resourcePath) {
        String rawText = readClasspathResource(resourcePath);
        ParsedKnowledgeSource parsed = parser.parse(rawText);
        if (isBlank(parsed.id())
                || isBlank(parsed.title())
                || isBlank(parsed.topic())
                || isBlank(parsed.body())) {
            throw new IllegalStateException("Invalid built-in knowledge source metadata or body: " + resourcePath);
        }
        return new KnowledgeSource(parsed.id(), parsed.title(), new Topic(parsed.topic()), parsed.body());
    }

    private List<KnowledgeSource> loadExternalSources(Path externalDirectory, Set<String> usedIds) {
        try (Stream<Path> entries = Files.list(externalDirectory)) {
            return entries
                    .filter(Files::isRegularFile)
                    .filter(this::isSupportedExternalNote)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .map(path -> loadExternalEntry(path, usedIds))
                    .flatMap(Optional::stream)
                    .toList();
        } catch (IOException | UncheckedIOException ex) {
            // 容错策略：外部目录是 Learner 的可选扩展，列目录失败（如无权限）不应影响内置资料可用。
            // 同时捕获 UncheckedIOException：Files.list 返回的流在遍历过程中出现 IO 错误时，会以该异常抛出。
            // 返回空列表后，调用方 load(Path) 只会得到内置默认集合，即“安全回退”。
            LOGGER.log(System.Logger.Level.WARNING,
                    "列出外部知识源目录失败，已回退为内置知识源：directory=" + externalDirectory, ex);
            return List.of();
        }
    }

    private boolean isSupportedExternalNote(Path entryPath) {
        Path namePath = entryPath.getFileName();
        if (namePath == null) {
            return false;
        }
        String entryName = namePath.toString();
        if (entryName.startsWith(".") || isOsHidden(entryPath)) {
            return false;
        }
        String lowerName = entryName.toLowerCase(Locale.ROOT);
        return lowerName.endsWith(".md") || lowerName.endsWith(".txt");
    }

    private boolean isOsHidden(Path entryPath) {
        try {
            return Files.isHidden(entryPath);
        } catch (IOException ex) {
            return false;
        }
    }

    private Optional<KnowledgeSource> loadExternalEntry(Path entryPath, Set<String> usedIds) {
        String rawText;
        try {
            rawText = Files.readString(entryPath, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            // 容错策略：单个笔记读取失败（权限不足、读取中途被删除等）只跳过该文件，
            // 返回 Optional.empty() 让上游 flatMap 自动丢弃，其余笔记与内置资料不受影响。
            LOGGER.log(System.Logger.Level.WARNING,
                    "跳过不可读的外部知识源文件：path=" + entryPath, ex);
            return Optional.empty();
        }

        ParsedKnowledgeSource parsed = parser.parse(rawText);
        if (isBlank(parsed.body())) {
            return Optional.empty();
        }

        String entryName = entryPath.getFileName().toString();
        String entryStem = stripExtension(entryName);
        boolean isMarkdown = entryName.toLowerCase(Locale.ROOT).endsWith(".md");

        String id = resolveUniqueId(parsed.id(), entryName, usedIds);
        String headingTitle = isMarkdown ? parser.extractFirstMarkdownHeading(parsed.body()) : null;
        String title = firstNonBlank(parsed.title(), headingTitle, entryStem, entryName.trim());
        String topicName = firstNonBlank(parsed.topic(), title);

        return Optional.of(new KnowledgeSource(id, title, new Topic(topicName), parsed.body()));
    }

    private String resolveUniqueId(String preferredId, String entryName, Set<String> usedIds) {
        if (!isBlank(preferredId) && usedIds.add(preferredId)) {
            return preferredId;
        }
        String baseId = "external:" + entryName;
        if (usedIds.add(baseId)) {
            return baseId;
        }
        int suffix = 2;
        while (!usedIds.add(baseId + "-" + suffix)) {
            suffix++;
        }
        return baseId + "-" + suffix;
    }

    private String readClasspathResource(String resourcePath) {
        ClassLoader classLoader = KnowledgeSourceLoader.class.getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalStateException("Built-in knowledge source not found on classpath: " + resourcePath);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read built-in knowledge source: " + resourcePath, ex);
        }
    }

    private String stripExtension(String entryName) {
        int lastDot = entryName.lastIndexOf('.');
        return lastDot > 0 ? entryName.substring(0, lastDot) : entryName;
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (!isBlank(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Expected at least one non-blank metadata candidate");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
