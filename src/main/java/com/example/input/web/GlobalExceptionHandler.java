package com.example.input.web;

import com.example.job.JobConflictException;
import java.nio.file.FileAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 접수 단계의 오류를 화면에 보여줄 수 있는 문장으로 바꾼다 (RFC 9457 ProblemDetail).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBadRequest(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(FileAlreadyExistsException.class)
    public ProblemDetail handleExists(FileAlreadyExistsException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "같은 이름의 프로젝트 폴더가 이미 있습니다: " + e.getFile() + " (다른 이름을 쓰거나 폴더를 옮기세요)");
    }

    @ExceptionHandler(JobConflictException.class)
    public ProblemDetail handleConflict(JobConflictException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }
}
