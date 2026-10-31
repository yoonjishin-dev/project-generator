package com.example.input.slack;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ResponseUrlNotifierTest {

    private static final String RESPONSE_URL = "https://hooks.slack.com/commands/T1/123/abc";

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final ResponseUrlNotifier notifier = new ResponseUrlNotifier(builder);

    @Test
    void response_url로_채널_메시지를_보낸다() {
        server.expect(requestTo(RESPONSE_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"response_type\":\"in_channel\",\"text\":\"완료\"}", true))
                .andRespond(withSuccess());

        notifier.send(RESPONSE_URL, new SlackMessage("완료", true));

        server.verify();
    }

    @Test
    void Slack_주소가_아니면_보내지_않는다() {
        notifier.send("http://169.254.169.254/latest/meta-data", new SlackMessage("완료", true));

        server.verify();
    }

    @Test
    void Slack이_오류를_돌려줘도_예외를_던지지_않는다() {
        server.expect(requestTo(RESPONSE_URL)).andRespond(withServerError());

        notifier.send(RESPONSE_URL, new SlackMessage("완료", false));

        server.verify();
    }
}
