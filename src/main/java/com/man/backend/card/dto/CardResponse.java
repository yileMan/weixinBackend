package com.man.backend.card.dto;

import com.man.backend.card.model.Card;
import com.man.backend.card.model.CardStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class CardResponse {

    private final Long id;
    private final String name;
    private final LocalDate purchaseDate;
    private final BigDecimal amount;
    private final CardStatus status;
    private final LocalDate inactiveDate;
    private final LocalDate saleDate;
    private final BigDecimal salePrice;
    private final LocalDateTime createdAt;
    private final Long usageDays;
    private final BigDecimal dailyAverage;
    private final String dailyAverageType;

    public CardResponse(Long id,
                        String name,
                        LocalDate purchaseDate,
                        BigDecimal amount,
                        CardStatus status,
                        LocalDate inactiveDate,
                        LocalDate saleDate,
                        BigDecimal salePrice,
                        LocalDateTime createdAt,
                        Long usageDays,
                        BigDecimal dailyAverage,
                        String dailyAverageType) {
        this.id = id;
        this.name = name;
        this.purchaseDate = purchaseDate;
        this.amount = amount;
        this.status = status;
        this.inactiveDate = inactiveDate;
        this.saleDate = saleDate;
        this.salePrice = salePrice;
        this.createdAt = createdAt;
        this.usageDays = usageDays;
        this.dailyAverage = dailyAverage;
        this.dailyAverageType = dailyAverageType;
    }

    public static CardResponse from(Card card) {
        Metrics metrics = calculateMetrics(card);
        return new CardResponse(
                card.getId(),
                card.getName(),
                card.getPurchaseDate(),
                card.getAmount(),
                card.getStatus(),
                card.getInactiveDate(),
                card.getSaleDate(),
                card.getSalePrice(),
                card.getCreatedAt(),
                metrics.usageDays,
                metrics.dailyAverage,
                metrics.dailyAverageType
        );
    }

    public Long getId() {
        return id;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getUsageDays() {
        return usageDays;
    }

    public BigDecimal getDailyAverage() {
        return dailyAverage;
    }

    public String getDailyAverageType() {
        return dailyAverageType;
    }

    private static Metrics calculateMetrics(Card card) {
        if (card.getStatus() == null || card.getPurchaseDate() == null) {
            return Metrics.empty();
        }
        if (card.getStatus() == CardStatus.ACTIVE) {
            return Metrics.empty();
        }

        LocalDate endDate = card.getStatus() == CardStatus.INACTIVE ? card.getInactiveDate() : card.getSaleDate();
        if (endDate == null) {
            return Metrics.empty();
        }

        long usageDays = ChronoUnit.DAYS.between(card.getPurchaseDate(), endDate) + 1;
        if (usageDays <= 0) {
            return Metrics.empty();
        }

        if (card.getStatus() == CardStatus.INACTIVE) {
            BigDecimal dailyAverage = card.getAmount()
                    .divide(BigDecimal.valueOf(usageDays), 2, RoundingMode.HALF_UP);
            return new Metrics(usageDays, dailyAverage, "COST");
        }

        BigDecimal salePrice = card.getSalePrice();
        if (salePrice == null) {
            return Metrics.empty();
        }
        BigDecimal dailyAverage = card.getAmount()
                .subtract(salePrice)
                .divide(BigDecimal.valueOf(usageDays), 2, RoundingMode.HALF_UP);
        String type = dailyAverage.signum() < 0 ? "PROFIT" : "COST";
        return new Metrics(usageDays, dailyAverage, type);
    }

    private static final class Metrics {
        private final Long usageDays;
        private final BigDecimal dailyAverage;
        private final String dailyAverageType;

        private Metrics(Long usageDays, BigDecimal dailyAverage, String dailyAverageType) {
            this.usageDays = usageDays;
            this.dailyAverage = dailyAverage;
            this.dailyAverageType = dailyAverageType;
        }

        private static Metrics empty() {
            return new Metrics(null, null, null);
        }
    }
}
