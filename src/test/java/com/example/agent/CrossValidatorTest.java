package com.example.agent;

import static com.example.agent.AgentTestSupport.OBJECT_MAPPER;
import static com.example.agent.AgentTestSupport.example;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.spec.ApiSpec;
import com.example.spec.ApiSpec.ResourceSpec;
import com.example.spec.Architecture;
import com.example.spec.Architecture.EntitySpec;
import com.example.spec.Architecture.FieldSpec;
import com.example.spec.Architecture.RelationSpec;
import com.example.spec.Architecture.RelationType;
import com.example.spec.FieldType;
import com.example.spec.Prd;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CrossValidatorTest {

    private final CrossValidator validator = new CrossValidator();
    private Prd prd;
    private Architecture architecture;
    private ApiSpec api;

    @BeforeEach
    void setUp() throws IOException {
        prd = OBJECT_MAPPER.readValue(example("prd.json"), Prd.class);
        architecture = OBJECT_MAPPER.readValue(example("architecture.json"), Architecture.class);
        api = OBJECT_MAPPER.readValue(example("api.json"), ApiSpec.class);
    }

    @Test
    void 예시는_모든_교차_검증을_통과한다() {
        assertThat(validator.checkPrd(prd)).isEmpty();
        assertThat(validator.checkArchitecture(prd, architecture)).isEmpty();
        assertThat(validator.checkApi(architecture, api)).isEmpty();
    }

    @Test
    void 기능_id가_중복되면_오류() {
        List<Prd.Feature> features = new ArrayList<>(prd.features());
        features.add(features.get(0));
        Prd duplicated = new Prd(prd.title(), prd.summary(), prd.goals(), prd.targetUsers(), features,
                prd.nonFunctionalRequirements(), prd.outOfScope());

        assertThat(validator.checkPrd(duplicated)).singleElement().asString().contains("F-001");
    }

    @Test
    void 없는_엔티티를_참조하거나_없는_기능을_가리키면_오류() {
        EntitySpec todo = entity("Todo");
        EntitySpec broken = new EntitySpec(todo.name(), todo.label(), todo.description(), List.of("F-001", "F-999"),
                todo.fields(), List.of(new RelationSpec(RelationType.MANY_TO_ONE, "owner", "User", true)));

        assertThat(validator.checkArchitecture(prd, new Architecture(List.of(entity("Category"), broken))))
                .anySatisfy(error -> assertThat(error).contains("'User'"))
                .anySatisfy(error -> assertThat(error).contains("F-999"));
    }

    @Test
    void 순환_관계는_경로와_함께_알려준다() {
        EntitySpec category = entity("Category");
        EntitySpec cyclic = new EntitySpec(category.name(), category.label(), category.description(),
                category.featureIds(), category.fields(),
                List.of(new RelationSpec(RelationType.MANY_TO_ONE, "todo", "Todo", false)));

        assertThat(validator.checkArchitecture(prd, new Architecture(List.of(cyclic, entity("Todo")))))
                .singleElement().asString().contains("Category → Todo → Category");
    }

    @Test
    void 자기_자신_참조와_같은_target_중복은_오류() {
        EntitySpec todo = entity("Todo");
        EntitySpec broken = new EntitySpec(todo.name(), todo.label(), todo.description(), todo.featureIds(),
                todo.fields(), List.of(
                new RelationSpec(RelationType.MANY_TO_ONE, "parent", "Todo", false),
                new RelationSpec(RelationType.MANY_TO_ONE, "category", "Category", false),
                new RelationSpec(RelationType.MANY_TO_ONE, "subCategory", "Category", false)));

        assertThat(validator.checkArchitecture(prd, new Architecture(List.of(entity("Category"), broken))))
                .anySatisfy(error -> assertThat(error).contains("자기 자신"))
                .anySatisfy(error -> assertThat(error).contains("두 번 참조"));
    }

    @Test
    void 관계가_자동으로_만드는_ID_필드를_따로_만들면_오류() {
        EntitySpec todo = entity("Todo");
        List<FieldSpec> fields = new ArrayList<>(todo.fields());
        fields.add(new FieldSpec("categoryId", "카테고리 ID", FieldType.STRING, false, false));
        EntitySpec broken = new EntitySpec(todo.name(), todo.label(), todo.description(), todo.featureIds(), fields,
                todo.relations());

        assertThat(validator.checkArchitecture(prd, new Architecture(List.of(entity("Category"), broken))))
                .anySatisfy(error -> assertThat(error).contains("'categoryId'").contains("자동으로 만드는 ID"))
                .anySatisfy(error -> assertThat(error).contains("컬럼 이름 'category_id'"));
    }

    @Test
    void 리소스가_빠지거나_남거나_경로가_겹치면_오류() {
        ApiSpec broken = new ApiSpec(List.of(
                api.resources().get(0),
                new ResourceSpec("Tag", api.resources().get(0).path(), api.resources().get(0).operations())));

        assertThat(validator.checkApi(architecture, broken))
                .anySatisfy(error -> assertThat(error).contains("'Tag'"))
                .anySatisfy(error -> assertThat(error).contains("'Todo'의 리소스가 없습니다"))
                .anySatisfy(error -> assertThat(error).contains("중복"));
    }

    private EntitySpec entity(String name) {
        return architecture.entities().stream().filter(entity -> entity.name().equals(name)).findFirst().orElseThrow();
    }
}
