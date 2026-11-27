package dev.javaagentstudy.common.knowledge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import dev.javaagentstudy.common.domain.KnowledgeSource;
import dev.javaagentstudy.common.domain.Topic;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * KnowledgeSourceLoader 公共接口的黑盒行为测试。
 */
class KnowledgeSourceLoaderTest {

    private final KnowledgeSourceLoader loader = new KnowledgeSourceLoader();

    @Test
    void loadsBuiltInJavaKnowledgeSourcesWhenNoExternalDirectoryIsGiven() {
        List<KnowledgeSource> sources = loader.load();

        assertThat(sources).hasSize(4);
        assertThat(sources).allSatisfy(source -> {
            assertThat(source.id()).isNotBlank();
            assertThat(source.title()).isNotBlank();
            assertThat(source.topic()).isNotNull();
            assertThat(source.topic().name()).isNotBlank();
            assertThat(source.content()).isNotBlank();
        });
        assertThat(sources)
                .extracting(KnowledgeSource::id)
                .doesNotHaveDuplicates();
        assertThat(sources)
                .extracting(KnowledgeSource::topic)
                .contains(
                        new Topic("集合框架"),
                        new Topic("JVM 内存基础"),
                        new Topic("并发入门"),
                        new Topic("Spring IoC 与 AOP"));
    }

    @Test
    void mergesExternalMarkdownAndTextNotesWithBuiltInDefaults(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("stream-notes.md"), """
                ---
                title: Stream API 常见陷阱
                topic: Stream 与 Lambda
                ---

                Stream 只能消费一次，重复终端操作会抛出 IllegalStateException。
                """, StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("exceptions.md"), """
                # 异常处理速记

                优先捕获具体异常，避免空的 catch 块吞掉故障现场。
                """, StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("gc-tips.TXT"),
                "生产环境建议开启 GC 日志并限制容器最大堆比例。", StandardCharsets.UTF_8);

        List<KnowledgeSource> builtIn = loader.load();
        List<KnowledgeSource> merged = loader.load(tempDir);

        assertThat(merged).hasSize(builtIn.size() + 3);
        assertThat(merged.subList(0, builtIn.size())).containsExactlyElementsOf(builtIn);

        List<KnowledgeSource> external = merged.subList(builtIn.size(), merged.size());
        assertThat(external)
                .extracting(KnowledgeSource::title, KnowledgeSource::topic)
                .containsExactlyInAnyOrder(
                        tuple("Stream API 常见陷阱", new Topic("Stream 与 Lambda")),
                        tuple("异常处理速记", new Topic("异常处理速记")),
                        tuple("gc-tips", new Topic("gc-tips")));
        assertThat(external).allSatisfy(source -> {
            assertThat(source.id()).isNotBlank();
            assertThat(source.content()).isNotBlank();
        });
        assertThat(merged)
                .extracting(KnowledgeSource::id)
                .doesNotHaveDuplicates();
    }

    @Test
    void ignoresUnsupportedExtensionsHiddenFilesSubdirectoriesAndBlankNotes(@TempDir Path tempDir)
            throws IOException {
        Files.writeString(tempDir.resolve("valid-note.md"), "# 泛型擦除\n编译后类型参数会被擦除。", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve(".hidden-note.md"), "# 隐藏笔记\n不应被加载。", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve(".DS_Store"), "macOS metadata", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("diagram.pdf"), "%PDF-1.4", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("Sample.java"), "class Sample {}", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("blank-note.txt"), "   \n\t  ", StandardCharsets.UTF_8);
        Path subDir = Files.createDirectory(tempDir.resolve("nested"));
        Files.writeString(subDir.resolve("nested.md"), "# 子目录笔记\n不应被扫描。", StandardCharsets.UTF_8);

        List<KnowledgeSource> builtIn = loader.load();
        List<KnowledgeSource> merged = loader.load(tempDir);

        assertThat(merged).hasSize(builtIn.size() + 1);
        assertThat(merged.getLast().title()).isEqualTo("泛型擦除");
        assertThat(merged.getLast().content()).contains("编译后类型参数会被擦除");
    }

    @Test
    void fallsBackToBuiltInDefaultsWhenExternalDirectoryIsNullMissingEmptyOrNotADirectory(@TempDir Path tempDir)
            throws IOException {
        List<KnowledgeSource> builtIn = loader.load();
        Path missingDir = tempDir.resolve("does-not-exist");
        Path emptyDir = Files.createDirectory(tempDir.resolve("empty-notes"));
        Path regularFile = Files.writeString(tempDir.resolve("single-file.md"), "# 单个文件", StandardCharsets.UTF_8);

        assertThat(loader.load(null)).containsExactlyElementsOf(builtIn);
        assertThat(loader.load(missingDir)).containsExactlyElementsOf(builtIn);
        assertThat(loader.load(emptyDir)).containsExactlyElementsOf(builtIn);
        assertThat(loader.load(regularFile)).containsExactlyElementsOf(builtIn);
    }

    @Test
    void avoidsDuplicateIdsAndHandlesWhitespaceStemExternalNotes(@TempDir Path tempDir) throws IOException {
        List<KnowledgeSource> builtIn = loader.load();
        String duplicatedBuiltInId = builtIn.getFirst().id();

        Files.writeString(tempDir.resolve("copied-template.md"), """
                ---
                id: %s
                title: 我的集合补充笔记
                topic: 集合框架
                ---

                补充记录 ArrayList 扩容 1.5 倍的细节。
                """.formatted(duplicatedBuiltInId), StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("   .txt"), "仅含空白主干文件名的纯文本笔记。", StandardCharsets.UTF_8);

        List<KnowledgeSource> merged = loader.load(tempDir);

        assertThat(merged).hasSize(builtIn.size() + 2);
        assertThat(merged)
                .extracting(KnowledgeSource::id)
                .doesNotHaveDuplicates();
        assertThat(merged).allSatisfy(source -> {
            assertThat(source.id()).isNotBlank();
            assertThat(source.title()).isNotBlank();
            assertThat(source.topic().name()).isNotBlank();
            assertThat(source.content()).isNotBlank();
        });
    }

    @Test
    void treatsEmptyFrontmatterAsMetadataInsteadOfBody(@TempDir Path tempDir) throws IOException {
        // 回归用例：frontmatter 两个分隔符紧挨着（中间没有任何字段）。
        // 期望解析为“空元信息 + 正文”，标题回退为文件名主干，而不是把 "---" 当成正文内容。
        Files.writeString(tempDir.resolve("empty-header.md"), "---\n---\n空头部之后的正文。", StandardCharsets.UTF_8);

        List<KnowledgeSource> merged = loader.load(tempDir);

        KnowledgeSource external = merged.getLast();
        assertThat(external.content()).isEqualTo("空头部之后的正文。");
        assertThat(external.title()).isEqualTo("empty-header");
    }

    @Test
    void skipsUnreadableExternalNoteAndKeepsOtherSources(@TempDir Path tempDir) throws IOException {
        Path good = Files.writeString(tempDir.resolve("good.md"), "# 可读笔记\n内容。", StandardCharsets.UTF_8);
        Path bad = Files.writeString(tempDir.resolve("bad.md"), "# 不可读笔记\n内容。", StandardCharsets.UTF_8);
        // 场景：目录里一个文件可读、一个文件不可读。
        // 通过去掉全部 POSIX 权限来模拟“读取失败”，期望加载器只跳过 bad.md，good.md 仍被合并。
        java.util.Set<java.nio.file.attribute.PosixFilePermission> none = java.util.Set.of();
        Files.setPosixFilePermissions(bad, none);
        try {
            org.junit.jupiter.api.Assumptions.assumeFalse(Files.isReadable(bad), "当前用户可读取无权限文件（如 root），跳过");

            List<KnowledgeSource> builtIn = loader.load();
            List<KnowledgeSource> merged = loader.load(tempDir);

            assertThat(merged).hasSize(builtIn.size() + 1);
            assertThat(merged.getLast().title()).isEqualTo("可读笔记");
        } finally {
            // 恢复权限，否则 @TempDir 在测试结束后无法清理该文件。
            Files.setPosixFilePermissions(bad, java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
            Files.deleteIfExists(good);
        }
    }

    @Test
    void fallsBackToBuiltInDefaultsWhenExternalDirectoryIsNotListable(@TempDir Path tempDir) throws IOException {
        Path lockedDir = Files.createDirectory(tempDir.resolve("locked"));
        Files.writeString(lockedDir.resolve("note.md"), "# 笔记\n内容。", StandardCharsets.UTF_8);
        // 场景：外部目录本身无法列出（权限被全部移除），期望安全回退为内置默认集合且不抛异常。
        Files.setPosixFilePermissions(lockedDir, java.util.Set.of());
        try {
            org.junit.jupiter.api.Assumptions.assumeFalse(Files.isReadable(lockedDir), "当前用户可读取无权限目录（如 root），跳过");

            assertThat(loader.load(lockedDir)).containsExactlyElementsOf(loader.load());
        } finally {
            // 恢复目录权限，保证 @TempDir 能正常清理。
            Files.setPosixFilePermissions(lockedDir, java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"));
        }
    }
}
