package com.man.backend.card.dto;

import java.util.List;

public class CardListResponse {

    private final List<CardResponse> items;

    public CardListResponse(List<CardResponse> items) {
        this.items = items;
    }

    public List<CardResponse> getItems() {
        return items;
    }
}
