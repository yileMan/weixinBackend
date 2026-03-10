package com.man.backend.card.service;

import com.man.backend.card.dto.CardRequest;
import com.man.backend.card.model.Card;
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
        validateCardRequest(request);
        AppUser user = userService.getOrCreateByOpenid(request.getOpenid().trim());
        Card card = new Card(null, request.getName().trim(), request.getPurchaseDate(), request.getAmount(), user);
        return cardRepository.save(card);
    }

    @Transactional
    public Optional<Card> update(Long id, CardRequest request) {
        validateCardRequest(request);
        String normalizedOpenid = request.getOpenid().trim();
        Optional<Card> cardOptional = cardRepository.findByIdAndUserOpenid(id, normalizedOpenid);
        if (cardOptional.isEmpty()) {
            log.warn("Update card not found: id={}, openid={}", id, maskOpenid(normalizedOpenid));
        }
        return cardOptional
                .map(card -> {
                    card.setName(request.getName().trim());
                    card.setPurchaseDate(request.getPurchaseDate());
                    card.setAmount(request.getAmount());
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

    private void validateCardRequest(CardRequest request) {
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
