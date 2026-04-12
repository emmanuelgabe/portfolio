package com.emmanuelgabe.portfolio.healthmonitor.action;

/**
 * Lifecycle state of a proposed agent action.
 * Transitions are: PENDING -> APPROVED -> EXECUTED (happy path),
 * PENDING -> REJECTED, PENDING -> EXPIRED, APPROVED -> FAILED.
 */
public enum ActionStatus {

    PENDING,
    APPROVED,
    REJECTED,
    EXECUTED,
    FAILED,
    EXPIRED
}
