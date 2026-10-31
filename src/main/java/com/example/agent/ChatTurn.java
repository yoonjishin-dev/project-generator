package com.example.agent;

/**
 * 대화 한 턴. 재요청할 때 직전 출력(ASSISTANT)과 오류 목록(USER)을 이어 붙이는 데 쓴다.
 */
public record ChatTurn(Role role, String content) {

    public enum Role {
        USER, ASSISTANT
    }

    public static ChatTurn user(String content) {
        return new ChatTurn(Role.USER, content);
    }

    public static ChatTurn assistant(String content) {
        return new ChatTurn(Role.ASSISTANT, content);
    }
}
