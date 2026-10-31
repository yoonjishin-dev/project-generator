package com.example.job;

/**
 * 같은 프로젝트명으로 진행 중인 작업이 이미 있다.
 */
public class JobConflictException extends RuntimeException {

    public JobConflictException(String message) {
        super(message);
    }
}
