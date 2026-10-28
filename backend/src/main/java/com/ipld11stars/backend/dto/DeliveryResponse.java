package com.ipld11stars.backend.dto;

import com.ipld11stars.backend.model.Delivery;

/**
 * Result of recording a delivery: the stored ball plus the innings situation after it.
 */
public record DeliveryResponse(Delivery delivery, String score, boolean inningsComplete, boolean freeHitNext) {
}
