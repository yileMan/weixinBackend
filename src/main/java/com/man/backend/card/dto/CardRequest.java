package com.man.backend.card.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CardRequest {

    private String openid;
    private String name;
    private LocalDate purchaseDate;
    private BigDecimal amount;

    public CardRequest() {
    }

    public String getOpenid() {
        return openid;
    }

    public String getName() {
        return name;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
