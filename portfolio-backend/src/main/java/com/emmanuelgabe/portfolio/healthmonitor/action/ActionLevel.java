package com.emmanuelgabe.portfolio.healthmonitor.action;

/**
 * Autonomy tier for a proposed agent action.
 * Governs the confirmation flow required before execution.
 */
public enum ActionLevel {

    /** Read-only observation, no human confirmation needed. Never persisted as an action. */
    GREEN,

    /** Recovery action requiring admin confirmation (optional reason). */
    ORANGE,

    /** Critical action requiring admin confirmation AND a mandatory reason. */
    RED
}
