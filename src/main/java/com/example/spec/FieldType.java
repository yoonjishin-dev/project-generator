package com.example.spec;

/**
 * 필드 타입과 생성 코드의 매핑. {@code rules.md}의 "필드 타입 매핑" 표와 같아야 한다.
 */
public enum FieldType {

    STRING("String", "length = 255", "varchar", "@NotBlank"),
    TEXT("String", "columnDefinition = \"TEXT\"", "text", "@NotBlank"),
    INTEGER("Integer", null, "int", "@NotNull"),
    LONG("Long", null, "bigint", "@NotNull"),
    DECIMAL("BigDecimal", "precision = 19, scale = 2", "decimal", "@NotNull"),
    BOOLEAN("Boolean", null, "boolean", "@NotNull"),
    DATE("LocalDate", null, "date", "@NotNull"),
    DATETIME("LocalDateTime", null, "datetime", "@NotNull");

    private final String javaType;
    private final String columnAttribute;
    private final String erType;
    private final String validation;

    FieldType(String javaType, String columnAttribute, String erType, String validation) {
        this.javaType = javaType;
        this.columnAttribute = columnAttribute;
        this.erType = erType;
        this.validation = validation;
    }

    public String javaType() {
        return javaType;
    }

    /** {@code @Column}에 추가로 붙는 속성. 없으면 {@code null}. */
    public String columnAttribute() {
        return columnAttribute;
    }

    public String erType() {
        return erType;
    }

    /** {@code required: true}일 때 Request에 붙는 검증 어노테이션. */
    public String validation() {
        return validation;
    }
}
