package com.example.input.slack;

/**
 * Slack에서 받은 slash command 하나.
 *
 * @param text        명령 뒤에 입력한 내용 (Slack이 {@code &}, {@code <}, {@code >}를 이스케이프해서 보낸다)
 * @param responseUrl 나중에 결과를 보낼 주소 (30분 동안 유효)
 */
public record SlackCommand(String userId, String text, String responseUrl) {
}
