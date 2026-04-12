package com.emmanuelgabe.portfolio.dto.health;

import jakarta.validation.constraints.Size;

/**
 * Admin payload for approving or rejecting a proposed action.
 *
 * @param reason optional for ORANGE approvals, mandatory for RED approvals
 *               and any rejection (enforced in the controller)
 */
public record ActionDecisionRequest(
        @Size(max = 1000) String reason
) { }
