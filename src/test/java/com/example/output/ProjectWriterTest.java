package com.example.output;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.render.GeneratedFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjectWriterTest {

    private final ProjectWriter writer = new ProjectWriter();

    @TempDir
    Path tempDir;

    @Test
    void 파일을_하위_디렉토리까지_만들어_쓴다() throws IOException {
        Path target = tempDir.resolve("todo-service");

        writer.write(target, List.of(text("README.md", "# hello\n"), text("src/main/App.java", "class App {}\n")));

        assertThat(target.resolve("README.md")).hasContent("# hello");
        assertThat(target.resolve("src/main/App.java")).hasContent("class App {}");
    }

    @Test
    void 실행_파일에는_실행_권한을_준다() throws IOException {
        Path target = tempDir.resolve("todo-service");

        writer.write(target, List.of(new GeneratedFile("gradlew", "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8), true)));

        assertThat(Files.isExecutable(target.resolve("gradlew"))).isTrue();
    }

    @Test
    void 빈_디렉토리에는_생성한다() throws IOException {
        Path target = Files.createDirectory(tempDir.resolve("todo-service"));

        writer.write(target, List.of(text("README.md", "# hello\n")));

        assertThat(target.resolve("README.md")).exists();
    }

    @Test
    void 이미_파일이_있는_디렉토리는_건드리지_않는다() throws IOException {
        Path target = Files.createDirectory(tempDir.resolve("todo-service"));
        Files.writeString(target.resolve("mine.txt"), "keep");

        assertThatThrownBy(() -> writer.write(target, List.of(text("README.md", "# hello\n"))))
                .isInstanceOf(FileAlreadyExistsException.class);
        assertThat(target.resolve("mine.txt")).hasContent("keep");
        assertThat(target.resolve("README.md")).doesNotExist();
    }

    @Test
    void 프로젝트_밖으로_나가는_경로가_있으면_아무것도_남기지_않는다() throws IOException {
        Path target = tempDir.resolve("todo-service");

        assertThatThrownBy(() -> writer.write(target, List.of(text("README.md", "# hello\n"), text("../evil.txt", "x"))))
                .isInstanceOf(IllegalArgumentException.class);
        try (Stream<Path> left = Files.list(tempDir)) {
            assertThat(left).isEmpty();
        }
    }

    private static GeneratedFile text(String path, String content) {
        return new GeneratedFile(path, content.getBytes(StandardCharsets.UTF_8), false);
    }
}
