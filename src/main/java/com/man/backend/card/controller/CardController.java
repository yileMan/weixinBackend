package com.man.backend.card.controller;

import com.man.backend.card.dto.CardRequest;
import com.man.backend.card.model.Card;
import com.man.backend.card.service.CardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private static final Logger log = LoggerFactory.getLogger(CardController.class);

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @GetMapping
    public List<Card> getCards(@RequestParam String openid) {
        log.info("GET /api/cards openid={}", maskOpenid(openid));
        return cardService.findAllByOpenid(openid);
    }

    @PostMapping
    public ResponseEntity<Card> createCard(@RequestBody CardRequest request) {
        log.info("POST /api/cards openid={}, name={}, purchaseDate={}, amount={}",
                maskOpenid(request == null ? null : request.getOpenid()),
                request == null ? null : request.getName(),
                request == null ? null : request.getPurchaseDate(),
                request == null ? null : request.getAmount());
        Card createdCard = cardService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCard);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Card> updateCard(@PathVariable Long id, @RequestBody CardRequest request) {
        log.info("PUT /api/cards/{} openid={}, name={}, purchaseDate={}, amount={}",
                id,
                maskOpenid(request == null ? null : request.getOpenid()),
                request == null ? null : request.getName(),
                request == null ? null : request.getPurchaseDate(),
                request == null ? null : request.getAmount());
        return cardService.update(id, request)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCard(@PathVariable Long id, @RequestParam String openid) {
        log.info("DELETE /api/cards/{} openid={}", id, maskOpenid(openid));
        if (cardService.delete(id, openid)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
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
