package com.example.input.slack;

import com.example.input.web.GenerationResponse;
import com.example.job.GenerationJob;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Slack 메시지 문구 (mrkdwn). 사용자나 AI가 만든 값은 {@link #escape}로 감싼다.
 */
final class SlackMessages {

    private static final int MAX_ERROR_DETAILS = 5;

    private SlackMessages() {
    }

    static String usage(String command) {
        return "사용법: `" + command + " <프로젝트명> <요구사항>`\n"
                + "예: `" + command + " todo-service 할 일을 등록, 수정, 삭제하고 카테고리로 분류하는 서비스`\n"
                + "프로젝트명은 영문 소문자로 시작하고 소문자·숫자·하이픈(-)만 쓸 수 있습니다.";
    }

    static String accepted(GenerationJob.Snapshot job, String webUrl) {
        return "*" + escape(job.projectName()) + "* 생성 요청을 접수했습니다. 보통 1~3분 걸리고, 끝나면 이 채널에 알려드릴게요.\n"
                + "진행 상황: <" + jobUrl(webUrl, job) + "|웹에서 보기>";
    }

    static String rejected(String reason, String command) {
        return "접수하지 못했습니다: " + escape(reason) + "\n" + usage(command);
    }

    static String finished(GenerationJob.Snapshot job, String userId, String webUrl) {
        GenerationResponse response = GenerationResponse.from(job);
        String requester = "<@" + userId + "> 님이 요청한 *" + escape(job.projectName()) + "*";
        String elapsed = elapsed(job);
        String link = "<" + jobUrl(webUrl, job) + "|웹에서 보기>";

        if (job.status() == GenerationJob.Status.SUCCEEDED) {
            GenerationResponse.Result result = response.result();
            String entities = result.entities().stream()
                    .map(entity -> "• " + escape(entity.label()) + " `" + escape(entity.path()) + "` ("
                            + entity.operations().stream().map(Enum::name).collect(Collectors.joining(", ")) + ")")
                    .collect(Collectors.joining("\n"));
            return ":white_check_mark: " + requester + " 생성이 끝났습니다 (" + elapsed + ")\n"
                    + "*" + escape(result.title()) + "* — 파일 " + result.fileCount() + "개\n"
                    + entities + "\n"
                    + "위치: `" + escape(result.projectDir()) + "` (생성기를 실행 중인 PC) · " + link;
        }

        String step = response.steps().stream()
                .filter(s -> s.state() == GenerationResponse.StepState.FAILED)
                .map(GenerationResponse.Step::label)
                .findFirst()
                .orElse("접수 직후");
        List<String> details = job.errorDetails();
        String detailText = details.stream().limit(MAX_ERROR_DETAILS)
                .map(detail -> "• " + escape(detail))
                .collect(Collectors.joining("\n"));
        if (details.size() > MAX_ERROR_DETAILS) {
            detailText += "\n• 외 " + (details.size() - MAX_ERROR_DETAILS) + "개";
        }
        return ":x: " + requester + " 생성에 실패했습니다 (" + step + " 단계, " + elapsed + ")\n"
                + escape(job.errorMessage())
                + (detailText.isEmpty() ? "" : "\n" + detailText)
                + "\n" + link;
    }

    /** Slack mrkdwn에서 특별한 의미가 있는 세 글자만 이스케이프한다. */
    static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Slack이 이스케이프해서 보낸 입력을 원래 글자로 되돌린다. */
    static String unescape(String text) {
        return text.replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
    }

    private static String jobUrl(String webUrl, GenerationJob.Snapshot job) {
        return webUrl + "/?job=" + job.id();
    }

    private static String elapsed(GenerationJob.Snapshot job) {
        if (job.startedAt() == null || job.finishedAt() == null) {
            return "-";
        }
        long seconds = Duration.between(job.startedAt(), job.finishedAt()).toSeconds();
        return seconds >= 60 ? (seconds / 60) + "분 " + (seconds % 60) + "초" : seconds + "초";
    }
}
