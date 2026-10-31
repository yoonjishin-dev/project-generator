package com.example.template;

import com.example.config.GeneratorProperties;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.TagOpt;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 생성에 쓸 템플릿 위치를 정한다.
 *
 * <ul>
 *   <li>기본: {@code generator.templates-dir} 폴더(이 저장소의 {@code project-templates/})를 그대로 쓴다.
 *       템플릿이 생성기와 같은 커밋에 있으므로 생성기 커밋이 같으면 같은 템플릿이 쓰인다.</li>
 *   <li>{@code generator.templates-repository}를 설정하면(템플릿을 별도 저장소로 분리했을 때)
 *       그 저장소의 {@code template-version} 태그를 받아 쓴다. 브랜치는 내용이 바뀌므로 받지 않는다.
 *       캐시는 {@code <templates-cache-dir>/<저장소 해시>/<태그>/}이고, 한 번 받은 태그는 네트워크 없이 다시 쓴다.</li>
 * </ul>
 */
@Slf4j
@Component
public class TemplateRepository {

    private static final Pattern VERSION = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*");

    private final GeneratorProperties properties;
    private TemplateCheckout checkout;

    public TemplateRepository(GeneratorProperties properties) {
        this.properties = properties;
    }

    /** 첫 생성 요청 전에 미리 받아 둔다. 실패해도 앱은 띄우고, 생성할 때 다시 시도한다. */
    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        try {
            checkout();
        } catch (RuntimeException e) {
            log.error("템플릿을 준비하지 못했습니다. 생성 요청 때 다시 시도합니다: {}", e.getMessage());
        }
    }

    public synchronized TemplateCheckout checkout() {
        if (checkout == null) {
            checkout = properties.useTemplatesRepository() ? fetch() : local();
            log.info("템플릿 준비: {}", checkout);
        }
        return checkout;
    }

    public Path root() {
        return checkout().root();
    }

    public Path templateDir() {
        return root().resolve("templates").resolve(properties.template());
    }

    private TemplateCheckout local() {
        Path root = properties.templatesDir();
        if (root == null || !Files.isDirectory(root)) {
            throw new IllegalStateException("템플릿 폴더가 없습니다: " + (root == null ? "(설정 안 됨)" : root.toAbsolutePath())
                    + ". 생성기 프로젝트 폴더에서 실행하거나 generator.templates-dir를 설정하세요");
        }
        return new TemplateCheckout(root, properties.templateVersion(), null);
    }

    private TemplateCheckout fetch() {
        String version = properties.templateVersion();
        if (version == null || !VERSION.matcher(version).matches()) {
            throw new IllegalStateException("template-version이 올바른 태그 이름이 아닙니다: " + version);
        }
        String uri = resolveUri(properties.templatesRepository());
        Path target = properties.templatesCacheDir().resolve(cacheKey(uri)).resolve(version).toAbsolutePath();
        try {
            if (Files.isDirectory(target)) {
                ObjectId cached = verifyCached(target, version);
                if (cached != null) {
                    return new TemplateCheckout(target, version, cached.getName());
                }
                log.warn("템플릿 캐시가 태그 {}와 다릅니다. 다시 받습니다: {}", version, target);
                deleteRecursively(target);
            }
            return new TemplateCheckout(target, version, cloneTag(uri, version, target).getName());
        } catch (IOException e) {
            throw new UncheckedIOException("템플릿 캐시를 만들 수 없습니다: " + target, e);
        } catch (GitAPIException e) {
            throw new IllegalStateException("템플릿 저장소를 받을 수 없습니다: " + uri + " (" + e.getMessage() + ")", e);
        }
    }

    /** 임시 디렉토리에 태그를 받은 뒤 한 번에 옮긴다. 중간에 실패해도 반쯤 받은 캐시가 남지 않는다. */
    private static ObjectId cloneTag(String uri, String version, Path target) throws IOException, GitAPIException {
        requireRemoteTag(uri, version);
        Files.createDirectories(target.getParent());
        Path staging = target.resolveSibling("." + version + ".tmp-" + UUID.randomUUID());
        try {
            ObjectId commit;
            // 태그를 branch로 주면 그 커밋에서 detached HEAD로 체크아웃된다
            try (Git git = Git.cloneRepository()
                    .setURI(uri)
                    .setDirectory(staging.toFile())
                    .setBranch(Constants.R_TAGS + version)
                    .setTagOption(TagOpt.FETCH_TAGS)
                    .call()) {
                commit = tagCommit(git.getRepository(), version);
                if (commit == null || !commit.equals(git.getRepository().resolve(Constants.HEAD))) {
                    throw new IllegalStateException("태그 " + version + "를 체크아웃하지 못했습니다");
                }
            }
            Files.move(staging, target, StandardCopyOption.ATOMIC_MOVE);
            return commit;
        } catch (IOException | GitAPIException | RuntimeException e) {
            deleteRecursively(staging);
            throw e;
        }
    }

    /** 브랜치 이름이나 없는 태그면, 받기 전에 있는 태그 목록과 함께 알려준다. */
    private static void requireRemoteTag(String uri, String version) throws GitAPIException {
        Collection<Ref> tags = Git.lsRemoteRepository().setRemote(uri).setTags(true).setHeads(false).call();
        String ref = Constants.R_TAGS + version;
        if (tags.stream().noneMatch(tag -> tag.getName().equals(ref))) {
            String names = tags.stream()
                    .map(tag -> tag.getName().substring(Constants.R_TAGS.length()))
                    .filter(name -> !name.endsWith("^{}"))
                    .sorted()
                    .collect(Collectors.joining(", "));
            throw new IllegalStateException("템플릿 저장소에 태그 " + version + "가 없습니다 (있는 태그: "
                    + (names.isEmpty() ? "없음" : names) + "). 브랜치 이름은 쓸 수 없습니다");
        }
    }

    /** 캐시가 태그 커밋 그대로면 그 커밋, 다른 커밋이거나 파일이 수정됐으면 null. */
    private static ObjectId verifyCached(Path dir, String version) throws IOException, GitAPIException {
        try (Git git = Git.open(dir.toFile())) {
            Repository repository = git.getRepository();
            ObjectId commit = tagCommit(repository, version);
            ObjectId head = repository.resolve(Constants.HEAD);
            boolean intact = commit != null && commit.equals(head) && git.status().call().isClean();
            return intact ? commit : null;
        } catch (RepositoryNotFoundException e) {
            return null;
        }
    }

    private static ObjectId tagCommit(Repository repository, String version) throws IOException {
        Ref ref = repository.exactRef(Constants.R_TAGS + version);
        if (ref == null) {
            return null;
        }
        Ref peeled = repository.getRefDatabase().peel(ref);
        // 주석 태그(git tag -a)는 태그 객체를 거쳐 커밋을 가리킨다
        return peeled.getPeeledObjectId() != null ? peeled.getPeeledObjectId() : peeled.getObjectId();
    }

    /** URL이 아니면 로컬 경로로 보고 절대 경로로 바꾼다. */
    static String resolveUri(String repository) {
        if (repository == null || repository.isBlank()) {
            throw new IllegalStateException("generator.templates-repository를 설정하세요");
        }
        if (repository.contains("://") || repository.matches("[\\w.-]+@[\\w.-]+:.*")) {
            return repository;
        }
        return Path.of(repository).toAbsolutePath().normalize().toString();
    }

    private static String cacheKey(String uri) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(uri.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 6);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void deleteRecursively(Path dir) {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        } catch (IOException e) {
            // 원래 예외를 덮지 않도록 정리 실패는 무시한다
        }
    }
}
