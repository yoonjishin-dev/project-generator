package com.example.input.slack;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * slash command의 response_url로 메시지를 보낸다. 봇을 채널에 초대하지 않아도 되고 별도 권한도 필요 없다.
 * 알림 실패가 생성 작업을 실패로 만들면 안 되므로, 예외를 던지지 않고 로그만 남긴다.
 */
@Slf4j
@Component
public class ResponseUrlNotifier implements SlackNotifier {

    // response_url은 Slack이 준 값이지만, 다른 곳으로 요청을 보내지 않도록 주소를 확인한다
    private static final String SLACK_HOOKS = "https://hooks.slack.com/";

    private final RestClient restClient;

    public ResponseUrlNotifier(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @Override
    public void send(String responseUrl, SlackMessage message) {
        if (responseUrl == null || !responseUrl.startsWith(SLACK_HOOKS)) {
            log.warn("Slack response_url이 아니라서 보내지 않습니다: {}", responseUrl);
            return;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("response_type", message.inChannel() ? "in_channel" : "ephemeral");
        body.put("text", message.text());
        try {
            restClient.post()
                    .uri(responseUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Slack 알림을 보내지 못했습니다", e);
        }
    }
}
