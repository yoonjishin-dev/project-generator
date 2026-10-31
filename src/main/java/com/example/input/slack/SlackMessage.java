package com.example.input.slack;

/**
 * response_url로 보내는 메시지.
 *
 * @param inChannel true면 채널 전체에 보이고, false면 명령을 입력한 사람에게만 보인다
 */
public record SlackMessage(String text, boolean inChannel) {
}
