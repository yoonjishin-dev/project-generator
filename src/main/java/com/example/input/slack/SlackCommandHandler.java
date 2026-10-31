package com.example.input.slack;

import com.example.job.GenerationJob;
import com.example.job.GenerationJobService;
import com.example.job.JobConflictException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * {@code /generate <프로젝트명> <요구사항>}을 해석해 생성 작업으로 접수한다.
 * Slack은 3초 안에 응답을 받아야 하므로 여기서는 접수만 하고, 결과는 끝난 뒤 response_url로 보낸다.
 */
@Component
@RequiredArgsConstructor
public class SlackCommandHandler {

    private final GenerationJobService jobService;
    private final SlackNotifier notifier;
    private final SlackProperties properties;

    /** @return 명령을 입력한 사람에게만 바로 보이는 응답 */
    public String handle(SlackCommand command) {
        if (!properties.allowedUserIds().isEmpty() && !properties.allowedUserIds().contains(command.userId())) {
            return "이 명령을 쓸 권한이 없습니다. 생성기 관리자에게 SLACK_ALLOWED_USER_IDS에 추가해 달라고 요청하세요.";
        }
        String text = command.text() == null ? "" : SlackMessages.unescape(command.text()).strip();
        if (text.isEmpty() || text.equalsIgnoreCase("help") || text.equals("도움말")) {
            return SlackMessages.usage(properties.command());
        }
        String[] parts = text.split("\\s+", 2);
        if (parts.length < 2 || parts[1].isBlank()) {
            return SlackMessages.rejected("요구사항을 입력하세요", properties.command());
        }

        try {
            GenerationJob.Snapshot job = jobService.submit(parts[0], parts[1].strip(), finished ->
                    notifier.send(command.responseUrl(), new SlackMessage(
                            SlackMessages.finished(finished, command.userId(), properties.webUrl()), true)));
            return SlackMessages.accepted(job, properties.webUrl());
        } catch (FileAlreadyExistsException e) {
            return SlackMessages.rejected("같은 이름의 프로젝트 폴더가 이미 있습니다. 다른 이름을 쓰세요", properties.command());
        } catch (IllegalArgumentException | JobConflictException e) {
            return SlackMessages.rejected(e.getMessage(), properties.command());
        } catch (IOException e) {
            return SlackMessages.rejected("생성 위치를 확인할 수 없습니다: " + e.getMessage(), properties.command());
        }
    }
}
