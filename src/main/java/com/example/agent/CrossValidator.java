package com.example.agent;

import com.example.spec.ApiSpec;
import com.example.spec.ApiSpec.ResourceSpec;
import com.example.spec.Architecture;
import com.example.spec.Architecture.EntitySpec;
import com.example.spec.Architecture.FieldSpec;
import com.example.spec.Architecture.RelationSpec;
import com.example.render.Names;
import com.example.spec.Prd;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 스키마만으로는 확인할 수 없는 규칙 ({@code rules.md} "교차 검증 규칙").
 * 오류 메시지는 에이전트에게 그대로 전달되므로 무엇을 고쳐야 하는지 구체적으로 쓴다.
 */
@Component
public class CrossValidator {

    public List<String> checkPrd(Prd prd) {
        List<String> errors = new ArrayList<>();
        for (String id : duplicates(prd.features().stream().map(Prd.Feature::id).toList())) {
            errors.add("features: 기능 id '" + id + "'가 중복됩니다. id는 F-001부터 겹치지 않게 붙이세요.");
        }
        return errors;
    }

    public List<String> checkArchitecture(Prd prd, Architecture architecture) {
        List<String> errors = new ArrayList<>();
        List<EntitySpec> entities = architecture.entities();
        Set<String> names = entities.stream().map(EntitySpec::name).collect(Collectors.toSet());
        Set<String> featureIds = prd.features().stream().map(Prd.Feature::id).collect(Collectors.toSet());

        for (String name : duplicates(entities.stream().map(EntitySpec::name).toList())) {
            errors.add("entities: 엔티티 이름 '" + name + "'가 중복됩니다.");
        }
        for (EntitySpec entity : entities) {
            String at = "entities[" + entity.name() + "]";
            List<String> ownNames = new ArrayList<>(entity.fields().stream().map(FieldSpec::name).toList());
            ownNames.addAll(entity.relations().stream().map(RelationSpec::fieldName).toList());
            for (String name : duplicates(ownNames)) {
                errors.add(at + ": 필드/관계 이름 '" + name + "'가 중복됩니다.");
            }
            for (RelationSpec relation : entity.relations()) {
                String idName = relation.fieldName() + "Id";
                if (entity.fields().stream().anyMatch(field -> field.name().equals(idName))) {
                    errors.add(at + ": 필드 '" + idName + "'는 관계 '" + relation.fieldName()
                            + "'가 자동으로 만드는 ID와 겹칩니다. 관계를 쓰면 ID 필드는 따로 만들지 말고 fields에서 빼세요.");
                }
            }
            for (String column : duplicates(columnNames(entity))) {
                errors.add(at + ": 컬럼 이름 '" + column + "'가 중복됩니다. 필드나 관계 이름을 바꾸세요.");
            }
            for (String target : duplicates(entity.relations().stream().map(RelationSpec::target).toList())) {
                errors.add(at + ": 같은 엔티티 '" + target + "'를 두 번 참조합니다. 관계는 target마다 하나만 두세요.");
            }
            for (RelationSpec relation : entity.relations()) {
                if (relation.target().equals(entity.name())) {
                    errors.add(at + ": 자기 자신을 참조하는 관계 '" + relation.fieldName() + "'는 만들 수 없습니다.");
                } else if (!names.contains(relation.target())) {
                    errors.add(at + ": 관계 '" + relation.fieldName() + "'의 target '" + relation.target()
                            + "'가 entities에 없습니다.");
                }
            }
            for (String featureId : entity.featureIds()) {
                if (!featureIds.contains(featureId)) {
                    errors.add(at + ": featureIds의 '" + featureId + "'가 PRD features에 없습니다.");
                }
            }
        }
        String cycle = findCycle(entities, names);
        if (cycle != null) {
            errors.add("entities: 관계에 순환이 있습니다 (" + cycle + "). 한쪽 관계를 없애세요.");
        }
        return errors;
    }

    public List<String> checkApi(Architecture architecture, ApiSpec api) {
        List<String> errors = new ArrayList<>();
        Set<String> names = architecture.entities().stream().map(EntitySpec::name).collect(Collectors.toSet());
        List<String> resourceEntities = api.resources().stream().map(ResourceSpec::entity).toList();

        for (String entity : duplicates(resourceEntities)) {
            errors.add("resources: 엔티티 '" + entity + "'의 리소스가 두 개 이상입니다. 엔티티마다 하나만 두세요.");
        }
        for (String entity : resourceEntities) {
            if (!names.contains(entity)) {
                errors.add("resources: '" + entity + "'는 architecture에 없는 엔티티입니다.");
            }
        }
        for (EntitySpec entity : architecture.entities()) {
            if (!resourceEntities.contains(entity.name())) {
                errors.add("resources: 엔티티 '" + entity.name() + "'의 리소스가 없습니다.");
            }
        }
        for (String path : duplicates(api.resources().stream().map(ResourceSpec::path).toList())) {
            errors.add("resources: path '" + path + "'가 중복됩니다.");
        }
        return errors;
    }

    /** 순환이 있으면 {@code A → B → A} 형태로, 없으면 {@code null}. 없는 target은 건너뛴다(별도 오류로 보고). */
    private static String findCycle(List<EntitySpec> entities, Set<String> names) {
        Map<String, List<String>> graph = new HashMap<>();
        for (EntitySpec entity : entities) {
            graph.put(entity.name(), entity.relations().stream()
                    .map(RelationSpec::target)
                    .filter(target -> names.contains(target) && !target.equals(entity.name()))
                    .toList());
        }
        Set<String> done = new HashSet<>();
        for (EntitySpec entity : entities) {
            String cycle = visit(entity.name(), graph, done, new LinkedHashSet<>());
            if (cycle != null) {
                return cycle;
            }
        }
        return null;
    }

    private static String visit(String name, Map<String, List<String>> graph, Set<String> done, LinkedHashSet<String> path) {
        if (done.contains(name)) {
            return null;
        }
        if (path.contains(name)) {
            List<String> nodes = new ArrayList<>(path);
            List<String> cycle = new ArrayList<>(nodes.subList(nodes.indexOf(name), nodes.size()));
            cycle.add(name);
            return String.join(" → ", cycle);
        }
        path.add(name);
        for (String target : graph.get(name)) {
            String cycle = visit(target, graph, done, path);
            if (cycle != null) {
                return cycle;
            }
        }
        path.remove(name);
        done.add(name);
        return null;
    }

    /** 템플릿이 만드는 컬럼 전체: id, 필드, 관계 조인 컬럼, created_at, updated_at. */
    private static List<String> columnNames(EntitySpec entity) {
        List<String> columns = new ArrayList<>(List.of("id", "created_at", "updated_at"));
        entity.fields().forEach(field -> columns.add(Names.snake(field.name())));
        entity.relations().forEach(relation -> columns.add(Names.snake(relation.fieldName()) + "_id"));
        return columns;
    }

    private static List<String> duplicates(List<String> values) {
        Set<String> seen = new HashSet<>();
        return values.stream().filter(value -> !seen.add(value)).distinct().toList();
    }
}
