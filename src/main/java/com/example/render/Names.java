package com.example.render;

import java.util.regex.Pattern;

/**
 * {@code rules.md} "네이밍" 규칙에 따른 이름 변환.
 */
public final class Names {

    private static final Pattern WORD_BOUNDARY = Pattern.compile("(?<!^)(?=[A-Z])");

    private Names() {
    }

    /** {@code dueDate} → {@code due_date}, {@code OrderItem} → {@code order_item} */
    public static String snake(String name) {
        return WORD_BOUNDARY.matcher(name).replaceAll("_").toLowerCase();
    }

    /** {@code Todo} → {@code todo} */
    public static String lowerFirst(String name) {
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    /** {@code dueDate} → {@code DueDate} */
    public static String upperFirst(String name) {
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
