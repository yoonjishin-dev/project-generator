package com.example.render;

import com.example.spec.ApiSpec.Operation;
import com.example.spec.ApiSpec.ResourceSpec;
import com.example.spec.Architecture.EntitySpec;
import com.example.spec.Architecture.FieldSpec;
import com.example.spec.Architecture.RelationSpec;
import com.example.spec.FieldType;
import com.example.spec.Prd;
import com.example.spec.ProjectName;
import com.example.spec.ProjectSpec;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 에이전트 출력 JSON을 정렬하고, 템플릿이 쓰는 계산 값을 더한다.
 * 계산 규칙은 {@code rules.md}의 "정렬 규칙", "렌더링 변수"와 같아야 한다.
 */
@Component
@RequiredArgsConstructor
public class RenderContextFactory {

    private static final String BASE_PACKAGE_PREFIX = "com.generated.";

    private final ObjectMapper objectMapper;

    public RenderContext create(ProjectSpec spec, String templateVersion) {
        String projectName = ProjectName.requireValid(spec.projectName());

        List<EntitySpec> entities = sortByDependency(spec.architecture().entities());
        Map<String, EntitySpec> entitiesByName = entities.stream()
                .collect(Collectors.toMap(EntitySpec::name, Function.identity()));
        Map<String, ResourceSpec> resourcesByEntity = spec.api().resources().stream()
                .collect(Collectors.toMap(ResourceSpec::entity, Function.identity()));

        List<Map<String, Object>> entityContexts = entities.stream()
                .map(entity -> entityContext(entity, entitiesByName, resourcesByEntity))
                .toList();
        markFirstLast(entityContexts);

        String basePackage = BASE_PACKAGE_PREFIX + projectName.replace("-", "");
        Map<String, Object> project = new LinkedHashMap<>();
        project.put("projectName", projectName);
        project.put("basePackage", basePackage);
        project.put("basePackagePath", basePackage.replace('.', '/'));
        project.put("databaseName", projectName.replace('-', '_'));
        project.put("templateVersion", templateVersion);
        project.put("prd", prdContext(spec.prd()));
        project.put("entities", entityContexts);
        project.put("entityPackagesText", entityContexts.stream()
                .map(entity -> "`" + basePackage + "." + entity.get("packageName") + "`")
                .collect(Collectors.joining(", ")));
        return new RenderContext(project, entityContexts);
    }

    private Map<String, Object> prdContext(Prd prd) {
        List<Prd.Feature> features = prd.features().stream()
                .sorted(Comparator.comparing(Prd.Feature::id))
                .toList();
        Prd sorted = new Prd(prd.title(), prd.summary(), prd.goals(), prd.targetUsers(), features,
                prd.nonFunctionalRequirements(), prd.outOfScope());
        return objectMapper.convertValue(sorted, new TypeReference<LinkedHashMap<String, Object>>() {
        });
    }

    /** 참조되는 엔티티가 먼저 오도록 정렬하고, 같은 단계면 이름순. */
    private List<EntitySpec> sortByDependency(List<EntitySpec> entities) {
        Map<String, List<String>> graph = new HashMap<>();
        for (EntitySpec entity : entities) {
            graph.put(entity.name(), entity.relations().stream().map(RelationSpec::target).toList());
        }
        Map<String, Integer> depths = new HashMap<>();
        for (EntitySpec entity : entities) {
            depth(entity.name(), graph, depths, new HashSet<>());
        }
        return entities.stream()
                .sorted(Comparator.<EntitySpec>comparingInt(entity -> depths.get(entity.name()))
                        .thenComparing(EntitySpec::name))
                .toList();
    }

    private int depth(String name, Map<String, List<String>> graph, Map<String, Integer> depths, Set<String> visiting) {
        Integer known = depths.get(name);
        if (known != null) {
            return known;
        }
        if (!visiting.add(name)) {
            throw new IllegalArgumentException("엔티티 관계에 순환이 있습니다: " + name);
        }
        int depth = 0;
        for (String target : graph.get(name)) {
            if (!graph.containsKey(target)) {
                throw new IllegalArgumentException(name + "이(가) 없는 엔티티를 참조합니다: " + target);
            }
            depth = Math.max(depth, depth(target, graph, depths, visiting) + 1);
        }
        visiting.remove(name);
        depths.put(name, depth);
        return depth;
    }

    private Map<String, Object> entityContext(EntitySpec entity, Map<String, EntitySpec> entitiesByName,
                                              Map<String, ResourceSpec> resourcesByEntity) {
        ResourceSpec resource = resourcesByEntity.get(entity.name());
        if (resource == null) {
            throw new IllegalArgumentException("API 리소스가 없는 엔티티입니다: " + entity.name());
        }
        Set<Operation> operations = EnumSet.noneOf(Operation.class);
        operations.addAll(resource.operations());

        List<Map<String, Object>> fields = new ArrayList<>();
        List<Map<String, Object>> relations = new ArrayList<>();
        List<Map<String, Object>> params = new ArrayList<>();
        Set<FieldType> fieldTypes = EnumSet.noneOf(FieldType.class);

        for (FieldSpec field : entity.fields()) {
            FieldType type = field.type();
            fieldTypes.add(type);
            String validation = field.required() ? type.validation() + " " : "";
            String capitalized = Names.upperFirst(field.name());

            Map<String, Object> context = new LinkedHashMap<>();
            context.put("name", field.name());
            context.put("label", field.label());
            context.put("type", type.name());
            context.put("required", field.required());
            context.put("unique", field.unique());
            context.put("capitalizedName", capitalized);
            context.put("javaType", type.javaType());
            context.put("columnName", Names.snake(field.name()));
            context.put("columnAnnotation", columnAnnotation(field));
            context.put("erType", type.erType());
            context.put("validationAnnotation", validation);
            fields.add(context);

            params.add(param(field.name(), type.javaType(), field.name(), type.javaType(), field.label(),
                    field.required(), false, validation, "entity.get" + capitalized + "()"));
        }

        List<RelationSpec> sortedRelations = entity.relations().stream()
                .sorted(Comparator.comparing(RelationSpec::fieldName))
                .toList();
        for (RelationSpec relation : sortedRelations) {
            EntitySpec target = entitiesByName.get(relation.target());
            String capitalized = Names.upperFirst(relation.fieldName());

            Map<String, Object> context = new LinkedHashMap<>();
            context.put("type", relation.type().name());
            context.put("fieldName", relation.fieldName());
            context.put("target", relation.target());
            context.put("required", relation.required());
            context.put("capitalizedFieldName", capitalized);
            context.put("targetVarName", Names.lowerFirst(relation.target()));
            context.put("targetPackageName", relation.target().toLowerCase());
            context.put("joinColumnName", Names.snake(relation.fieldName()) + "_id");
            context.put("ownerName", entity.name());
            context.put("targetLabel", target.label());
            relations.add(context);

            String responseExpression = relation.required()
                    ? "entity.get" + capitalized + "().getId()"
                    : "entity.get" + capitalized + "() != null ? entity.get" + capitalized + "().getId() : null";
            params.add(param(relation.fieldName(), relation.target(), relation.fieldName() + "Id", "Long",
                    target.label(), relation.required(), true, relation.required() ? "@NotNull " : "",
                    responseExpression));
        }

        boolean hasList = operations.contains(Operation.LIST);
        boolean hasGet = operations.contains(Operation.GET);
        boolean hasCreate = operations.contains(Operation.CREATE);
        boolean hasUpdate = operations.contains(Operation.UPDATE);
        boolean hasDelete = operations.contains(Operation.DELETE);
        boolean hasBody = hasCreate || hasUpdate;
        boolean hasId = hasGet || hasUpdate || hasDelete;
        boolean hasRelations = !relations.isEmpty();

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("name", entity.name());
        context.put("label", entity.label());
        context.put("description", entity.description());
        context.put("varName", Names.lowerFirst(entity.name()));
        context.put("packageName", entity.name().toLowerCase());
        context.put("tableName", Names.snake(entity.name()));
        context.put("path", resource.path());
        context.put("hasList", hasList);
        context.put("hasGet", hasGet);
        context.put("hasCreate", hasCreate);
        context.put("hasUpdate", hasUpdate);
        context.put("hasDelete", hasDelete);
        context.put("hasBody", hasBody);
        context.put("hasId", hasId);
        context.put("hasRelations", hasRelations);
        context.put("usesNotFound", hasId || (hasBody && hasRelations));
        context.put("importsBigDecimal", fieldTypes.contains(FieldType.DECIMAL));
        context.put("importsLocalDate", fieldTypes.contains(FieldType.DATE));
        context.put("importsLocalDateTime", fieldTypes.contains(FieldType.DATETIME));
        context.put("importsHttpStatus", hasCreate || hasDelete);
        context.put("featureIdsText", entity.featureIds().stream().sorted().collect(Collectors.joining(", ")));
        context.put("fields", markFirstLast(fields));
        context.put("relations", markFirstLast(relations));
        context.put("params", markFirstLast(params));
        return context;
    }

    private static String columnAnnotation(FieldSpec field) {
        List<String> attributes = new ArrayList<>();
        attributes.add("name = \"" + Names.snake(field.name()) + "\"");
        if (field.required()) {
            attributes.add("nullable = false");
        }
        if (field.unique()) {
            attributes.add("unique = true");
        }
        if (field.type().columnAttribute() != null) {
            attributes.add(field.type().columnAttribute());
        }
        return "@Column(" + String.join(", ", attributes) + ")";
    }

    private static Map<String, Object> param(String name, String javaType, String dtoName, String dtoType, String label,
                                             boolean required, boolean isRelation, String validationAnnotation,
                                             String responseExpression) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("name", name);
        context.put("javaType", javaType);
        context.put("dtoName", dtoName);
        context.put("dtoType", dtoType);
        context.put("label", label);
        context.put("required", required);
        context.put("isRelation", isRelation);
        context.put("validationAnnotation", validationAnnotation);
        context.put("responseExpression", responseExpression);
        return context;
    }

    private static List<Map<String, Object>> markFirstLast(List<Map<String, Object>> items) {
        for (int i = 0; i < items.size(); i++) {
            items.get(i).put("first", i == 0);
            items.get(i).put("last", i == items.size() - 1);
        }
        return items;
    }
}
