package com.man.backend.card.service;

import com.man.backend.card.dto.CardRequest;
import com.man.backend.card.model.Card;
import com.man.backend.card.model.CardStatus;
import com.man.backend.card.repository.CardRepository;
import com.man.backend.user.model.AppUser;
import com.man.backend.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class CardService {

    private static final Logger log = LoggerFactory.getLogger(CardService.class);

    private final CardRepository cardRepository;
    private final UserService userService;

    public CardService(CardRepository cardRepository, UserService userService) {
        this.cardRepository = cardRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<Card> findAllByOpenid(String openid) {
        String normalizedOpenid = requireNonBlank(openid, "openid is required");
        return cardRepository.findByUserOpenidOrderByIdAsc(normalizedOpenid);
    }

    @Transactional
    public Card create(CardRequest request) {
        validateBaseCardRequest(request);
        CardStatus targetStatus = normalizeCreateStatus(request);
        validateStatusFields(targetStatus, request.getPurchaseDate(), request.getInactiveDate(), request.getSaleDate(), request.getSalePrice());

        AppUser user = userService.getOrCreateByOpenid(request.getOpenid().trim());
        Card card = new Card(
                null,
                request.getName().trim(),
                request.getPurchaseDate(),
                request.getAmount(),
                targetStatus,
                request.getInactiveDate(),
                request.getSaleDate(),
                request.getSalePrice(),
                user
        );
        return cardRepository.save(card);
    }

    @Transactional
    public Optional<Card> update(Long id, CardRequest request) {
        validateBaseCardRequest(request);
        String normalizedOpenid = request.getOpenid().trim();
        Optional<Card> cardOptional = cardRepository.findByIdAndUserOpenid(id, normalizedOpenid);
        if (cardOptional.isEmpty()) {
            log.warn("Update card not found: id={}, openid={}", id, maskOpenid(normalizedOpenid));
        }
        return cardOptional
                .map(card -> {
                    CardStatus targetStatus = request.getStatus() != null
                            ? request.getStatus()
                            : defaultStatus(card.getStatus());
                    LocalDate targetInactiveDate = request.getStatus() != null || request.getInactiveDate() != null
                            ? request.getInactiveDate()
                            : card.getInactiveDate();
                    LocalDate targetSaleDate = request.getStatus() != null || request.getSaleDate() != null
                            ? request.getSaleDate()
                            : card.getSaleDate();
                    BigDecimal targetSalePrice = request.getStatus() != null || request.getSalePrice() != null
                            ? request.getSalePrice()
                            : card.getSalePrice();

                    validateStatusFields(
                            targetStatus,
                            request.getPurchaseDate(),
                            targetInactiveDate,
                            targetSaleDate,
                            targetSalePrice
                    );

                    card.setName(request.getName().trim());
                    card.setPurchaseDate(request.getPurchaseDate());
                    card.setAmount(request.getAmount());
                    card.setStatus(targetStatus);
                    card.setInactiveDate(targetInactiveDate);
                    card.setSaleDate(targetSaleDate);
                    card.setSalePrice(targetSalePrice);
                    return cardRepository.save(card);
                });
    }

    @Transactional
    public boolean delete(Long id, String openid) {
        String normalizedOpenid = requireNonBlank(openid, "openid is required");
        Optional<Card> cardOptional = cardRepository.findByIdAndUserOpenid(id, normalizedOpenid);
        if (cardOptional.isEmpty()) {
            log.warn("Delete card not found: id={}, openid={}", id, maskOpenid(normalizedOpenid));
            return false;
        }
        cardRepository.delete(cardOptional.get());
        return true;
    }

    private void validateBaseCardRequest(CardRequest request) {
        if (request == null) {
            log.warn("Card request body is null");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request body is required");
        }
        requireNonBlank(request.getOpenid(), "openid is required");
        requireNonBlank(request.getName(), "name is required");
        if (request.getPurchaseDate() == null) {
            log.warn("Card purchaseDate missing: openid={}, name={}",
                    maskOpenid(request.getOpenid()),
                    request.getName());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "purchaseDate is required");
        }
        if (request.getAmount() == null) {
            log.warn("Card amount missing: openid={}, name={}",
                    maskOpenid(request.getOpenid()),
                    request.getName());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount is required");
        }
    }

    private CardStatus normalizeCreateStatus(CardRequest request) {
        return request.getStatus() == null ? CardStatus.ACTIVE : request.getStatus();
    }

    private CardStatus defaultStatus(CardStatus status) {
        return status == null ? CardStatus.ACTIVE : status;
    }

    private void validateStatusFields(CardStatus status,
                                      LocalDate purchaseDate,
                                      LocalDate inactiveDate,
                                      LocalDate saleDate,
                                      BigDecimal salePrice) {
        if (status == null) {
            log.warn("Card status missing");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required");
        }

        switch (status) {
            case ACTIVE -> validateActiveStatus(inactiveDate, saleDate, salePrice);
            case INACTIVE -> validateInactiveStatus(purchaseDate, inactiveDate, saleDate, salePrice);
            case SOLD -> validateSoldStatus(purchaseDate, inactiveDate, saleDate, salePrice);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unsupported status");
        }
    }

    private void validateActiveStatus(LocalDate inactiveDate, LocalDate saleDate, BigDecimal salePrice) {
        if (inactiveDate != null || saleDate != null || salePrice != null) {
            log.warn("ACTIVE card contains unexpected lifecycle fields");
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "ACTIVE cards must not contain inactiveDate, saleDate or salePrice");
        }
    }

    private void validateInactiveStatus(LocalDate purchaseDate,
                                        LocalDate inactiveDate,
                                        LocalDate saleDate,
                                        BigDecimal salePrice) {
        if (inactiveDate == null) {
            log.warn("INACTIVE card missing inactiveDate");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "inactiveDate is required when status is INACTIVE");
        }
        if (saleDate != null || salePrice != null) {
            log.warn("INACTIVE card contains sale fields");
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "INACTIVE cards must not contain saleDate or salePrice");
        }
        validateEndDateNotBeforePurchase(purchaseDate, inactiveDate, "inactiveDate");
    }

    private void validateSoldStatus(LocalDate purchaseDate,
                                    LocalDate inactiveDate,
                                    LocalDate saleDate,
                                    BigDecimal salePrice) {
        if (inactiveDate != null) {
            log.warn("SOLD card contains inactiveDate");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SOLD cards must not contain inactiveDate");
        }
        if (saleDate == null) {
            log.warn("SOLD card missing saleDate");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "saleDate is required when status is SOLD");
        }
        if (salePrice == null) {
            log.warn("SOLD card missing salePrice");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "salePrice is required when status is SOLD");
        }
        if (salePrice.signum() < 0) {
            log.warn("SOLD card has negative salePrice: {}", salePrice);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "salePrice must be greater than or equal to 0");
        }
        validateEndDateNotBeforePurchase(purchaseDate, saleDate, "saleDate");
    }

    private void validateEndDateNotBeforePurchase(LocalDate purchaseDate, LocalDate endDate, String fieldName) {
        if (purchaseDate != null && endDate != null && endDate.isBefore(purchaseDate)) {
            log.warn("{} is before purchaseDate: purchaseDate={}, endDate={}", fieldName, purchaseDate, endDate);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be on or after purchaseDate");
        }
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            log.warn("Card request invalid: {}", message);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private String maskOpenid(String openid) {
        if (openid == null || openid.isBlank()) {
            return "blank";
        }
        String trimmed = openid.trim();
        if (trimmed.length() <= 6) {
            return "***";
        }
        return trimmed.substring(0, 3) + "***" + trimmed.substring(trimmed.length() - 3);
    }
}
