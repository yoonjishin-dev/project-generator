package com.example.input.slack;

import com.slack.api.app_backend.slash_commands.payload.SlashCommandPayload;
import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import com.slack.api.socket_mode.SocketModeClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * Socket Mode로 Slack에 접속해 slash command를 받는다. 앱이 먼저 접속하므로 공개 주소나 터널이 필요 없다.
 * 토큰이 없거나 접속에 실패해도 웹 화면은 그대로 쓸 수 있도록 앱 시작을 막지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlackSocketModeRunner implements SmartLifecycle {

    private final SlackProperties properties;
    private final SlackCommandHandler handler;

    private SocketModeApp socketModeApp;
    private volatile boolean running;

    @Override
    public void start() {
        running = true;
        if (!properties.enabled()) {
            log.info("Slack 연동 꺼짐: SLACK_BOT_TOKEN, SLACK_APP_TOKEN을 설정하면 {} 명령을 받을 수 있습니다", properties.command());
            return;
        }
        try {
            App app = new App(AppConfig.builder().singleTeamBotToken(properties.botToken()).build());
            app.command(properties.command(), (request, context) -> {
                SlashCommandPayload payload = request.getPayload();
                return context.ack(handler.handle(
                        new SlackCommand(payload.getUserId(), payload.getText(), payload.getResponseUrl())));
            });
            socketModeApp = new SocketModeApp(properties.appToken(), app, SocketModeClient.Backend.JavaWebSocket);
            socketModeApp.startAsync();
            log.info("Slack Socket Mode 접속: {} 명령을 받습니다", properties.command());
        } catch (Exception e) {
            log.error("Slack에 접속하지 못했습니다. 토큰과 Socket Mode 설정을 확인하세요 (웹 화면은 계속 쓸 수 있습니다)", e);
        }
    }

    @Override
    public void stop() {
        running = false;
        if (socketModeApp != null) {
            try {
                socketModeApp.close();
            } catch (Exception e) {
                log.warn("Slack 연결 종료 중 오류", e);
            }
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
