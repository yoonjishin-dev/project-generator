package com.example.input.slack;

/**
 * 접수 이후의 결과를 Slack에 보낸다.
 */
public interface SlackNotifier {

    void send(String responseUrl, SlackMessage message);
}
