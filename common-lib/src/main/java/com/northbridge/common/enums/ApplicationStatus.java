package com.northbridge.common.enums;

/**
 * Enum representing the lifecycle status of a current account application.
 */
public enum ApplicationStatus {
    SUBMITTED,
    UNDER_REVIEW,
    OFFER_READY,
    OFFER_ACCEPTED,
    AWAITING_SIGNATURE,
    SIGNED,
    PROVISIONING,
    ACTIVE,
    DECLINED,
    WITHDRAWN
}

