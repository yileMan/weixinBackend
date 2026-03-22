package com.man.backend.card.dto;

import com.man.backend.card.model.CardStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CardRequest {

    private String openid;
    private String name;
    private LocalDate purchaseDate;
    private BigDecimal amount;
    private CardStatus status;
    private LocalDate inactiveDate;
    private LocalDate saleDate;
    private BigDecimal salePrice;

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

    public CardStatus getStatus() {
        return status;
    }

    public LocalDate getInactiveDate() {
        return inactiveDate;
    }

    public LocalDate getSaleDate() {
        return saleDate;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
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

    public void setStatus(CardStatus status) {
        this.status = status;
    }

    public void setInactiveDate(LocalDate inactiveDate) {
        this.inactiveDate = inactiveDate;
    }

    public void setSaleDate(LocalDate saleDate) {
        this.saleDate = saleDate;
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }
}
