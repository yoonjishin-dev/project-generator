package com.example.output;

import com.example.render.GeneratedFile;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * 렌더링된 파일을 로컬 디렉토리에 쓴다.
 * 같은 위치의 임시 디렉토리에 모두 쓴 뒤 한 번에 옮기므로, 실패해도 일부만 생성된 프로젝트가 남지 않는다.
 */
@Component
public class ProjectWriter {

    /** 생성할 수 없는 위치면 예외. 오래 걸리는 에이전트 호출 전에 먼저 확인하는 용도. */
    public void ensureWritable(Path targetDir) throws IOException {
        Path target = targetDir.toAbsolutePath().normalize();
        if (Files.exists(target) && !isEmptyDirectory(target)) {
            throw new FileAlreadyExistsException(target.toString(), null, "이미 파일이 있는 디렉토리에는 생성하지 않습니다");
        }
    }

    public Path write(Path targetDir, List<GeneratedFile> files) throws IOException {
        ensureWritable(targetDir);
        Path target = targetDir.toAbsolutePath().normalize();
        Path parent = target.getParent();
        Files.createDirectories(parent);
        Path staging = parent.resolve("." + target.getFileName() + ".tmp-" + UUID.randomUUID());

        try {
            for (GeneratedFile file : files) {
                Path path = staging.resolve(file.path()).normalize();
                if (!path.startsWith(staging)) {
                    throw new IllegalArgumentException("프로젝트 밖으로 나가는 경로입니다: " + file.path());
                }
                Files.createDirectories(path.getParent());
                Files.write(path, file.content());
                if (file.executable() && !path.toFile().setExecutable(true, false)) {
                    throw new IOException("실행 권한을 줄 수 없습니다: " + path);
                }
            }
            Files.deleteIfExists(target);
            Files.move(staging, target, StandardCopyOption.ATOMIC_MOVE);
            return target;
        } catch (IOException | RuntimeException e) {
            deleteRecursively(staging);
            throw e;
        }
    }

    private static boolean isEmptyDirectory(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return false;
        }
        try (Stream<Path> entries = Files.list(dir)) {
            return entries.findAny().isEmpty();
        }
    }

    private static void deleteRecursively(Path dir) {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException | UncheckedIOException e) {
            // 원래 예외를 덮지 않도록 정리 실패는 무시한다
        }
    }
}
