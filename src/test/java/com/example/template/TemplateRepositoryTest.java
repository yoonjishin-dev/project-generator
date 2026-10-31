package com.example.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.config.GeneratorProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TemplateRepositoryTest {

    private static final PersonIdent AUTHOR = new PersonIdent("test", "test@example.com");

    @TempDir
    Path temp;

    Path source;
    Path cache;
    RevCommit v100;
    RevCommit v110;

    /** v1.0.0(주석 태그) → v1.1.0(가벼운 태그) → 아직 태그 없는 main 순서로 커밋한 저장소. */
    @BeforeEach
    void 템플릿_저장소를_만든다() throws IOException, GitAPIException {
        source = temp.resolve("project-templates");
        cache = temp.resolve("cache");
        try (Git git = Git.init().setDirectory(source.toFile()).setInitialBranch("main").call()) {
            v100 = commit(git, "1.0.0");
            git.tag().setName("v1.0.0").setMessage("첫 버전").setTagger(AUTHOR).call();
            v110 = commit(git, "1.1.0");
            git.tag().setName("v1.1.0").setAnnotated(false).call();
            commit(git, "작업 중");
        }
    }

    @Test
    void 브랜치_최신이_아니라_태그_시점의_템플릿을_받는다() throws IOException {
        TemplateCheckout checkout = repository("v1.0.0").checkout();

        assertThat(Files.readString(checkout.root().resolve("rules.md"))).isEqualTo("1.0.0");
        assertThat(checkout.version()).isEqualTo("v1.0.0");
        assertThat(checkout.commitId()).isEqualTo(v100.getName());
        assertThat(checkout.root()).startsWith(cache.toAbsolutePath());
    }

    @Test
    void 가벼운_태그도_받는다() throws IOException {
        TemplateCheckout checkout = repository("v1.1.0").checkout();

        assertThat(Files.readString(checkout.root().resolve("rules.md"))).isEqualTo("1.1.0");
        assertThat(checkout.commitId()).isEqualTo(v110.getName());
    }

    @Test
    void 버전마다_따로_캐시한다() {
        assertThat(repository("v1.0.0").root()).isNotEqualTo(repository("v1.1.0").root());
    }

    @Test
    void 브랜치_이름이나_없는_태그는_거부하고_있는_태그를_알려준다() {
        assertThatThrownBy(() -> repository("main").checkout())
                .hasMessageContaining("태그 main가 없습니다")
                .hasMessageContaining("v1.0.0, v1.1.0");
        assertThatThrownBy(() -> repository("v9.9.9").checkout()).hasMessageContaining("태그 v9.9.9가 없습니다");
        assertThat(cache).as("받기 전에 거부하므로 캐시가 생기지 않는다").doesNotExist();
    }

    @Test
    void 경로로_쓸_수_없는_버전은_거부한다() {
        assertThatThrownBy(() -> repository("../v1.0.0").checkout()).hasMessageContaining("올바른 태그 이름이 아닙니다");
    }

    @Test
    void 한번_받은_태그는_저장소_없이도_캐시로_쓴다() throws IOException {
        Path first = repository("v1.0.0").root();
        deleteRecursively(source);

        TemplateCheckout again = repository("v1.0.0").checkout();
        assertThat(again.root()).isEqualTo(first);
        assertThat(again.commitId()).isEqualTo(v100.getName());
    }

    @Test
    void 캐시가_수정됐으면_다시_받는다() throws IOException {
        Path root = repository("v1.0.0").root();
        Files.writeString(root.resolve("rules.md"), "누군가 고침");

        TemplateCheckout again = repository("v1.0.0").checkout();
        assertThat(Files.readString(again.root().resolve("rules.md"))).isEqualTo("1.0.0");
    }

    @Test
    void 저장소를_설정하지_않으면_폴더를_그대로_쓴다() {
        GeneratorProperties properties = new GeneratorProperties(
                source, "", cache, "spring-boot", "v1.0.0", temp, 2);
        TemplateCheckout checkout = new TemplateRepository(properties).checkout();

        assertThat(checkout.root()).isEqualTo(source);
        assertThat(checkout.version()).isEqualTo("v1.0.0");
        assertThat(checkout.commitId()).isNull();
        assertThat(cache).doesNotExist();
    }

    @Test
    void 템플릿_폴더가_없으면_알려준다() {
        GeneratorProperties properties = new GeneratorProperties(
                temp.resolve("없는-폴더"), null, cache, "spring-boot", "v1.0.0", temp, 2);

        assertThatThrownBy(() -> new TemplateRepository(properties).checkout())
                .hasMessageContaining("템플릿 폴더가 없습니다");
    }

    @Test
    void 로컬_경로는_절대_경로로_URL은_그대로() {
        assertThat(TemplateRepository.resolveUri("../project-templates"))
                .isEqualTo(Path.of("../project-templates").toAbsolutePath().normalize().toString());
        assertThat(TemplateRepository.resolveUri("https://github.com/a/b.git")).isEqualTo("https://github.com/a/b.git");
        assertThat(TemplateRepository.resolveUri("git@github.com:a/b.git")).isEqualTo("git@github.com:a/b.git");
    }

    private TemplateRepository repository(String version) {
        return new TemplateRepository(new GeneratorProperties(
                temp.resolve("unused"), source.toString(), cache, "spring-boot", version, temp.resolve("out"), 2));
    }

    private RevCommit commit(Git git, String content) throws IOException, GitAPIException {
        Files.writeString(source.resolve("rules.md"), content);
        git.add().addFilepattern(".").call();
        return git.commit().setMessage(content).setAuthor(AUTHOR).setCommitter(AUTHOR).setSign(false).call();
    }

    private static void deleteRecursively(Path dir) throws IOException {
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        }
    }
}
