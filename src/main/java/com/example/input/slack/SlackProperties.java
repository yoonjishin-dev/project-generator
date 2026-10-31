package com.example.input.slack;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code application.yml}의 {@code generator.slack.*} 설정.
 *
 * @param botToken       봇 토큰 ({@code xoxb-}). Bolt가 요청을 인증할 때 쓴다
 * @param appToken       앱 수준 토큰 ({@code xapp-}, {@code connections:write}). Socket Mode 접속에 쓴다
 * @param command        Slack 앱에 등록한 slash command 이름
 * @param allowedUserIds 명령을 쓸 수 있는 Slack 사용자 ID. 비어 있으면 워크스페이스의 누구나
 * @param webUrl         메시지에 넣을 웹 화면 주소
 */
@ConfigurationProperties("generator.slack")
public record SlackProperties(
        String botToken,
        String appToken,
        String command,
        List<String> allowedUserIds,
        String webUrl
) {

    public SlackProperties {
        allowedUserIds = allowedUserIds == null ? List.of() : List.copyOf(allowedUserIds);
    }

    /** 두 토큰이 모두 있어야 Slack에 접속한다. */
    public boolean enabled() {
        return botToken != null && !botToken.isBlank() && appToken != null && !appToken.isBlank();
    }
}
