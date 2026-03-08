package com.man.backend.card.service;

import com.man.backend.card.dto.CardRequest;
import com.man.backend.card.model.Card;
import com.man.backend.card.repository.CardRepository;
import com.man.backend.user.model.AppUser;
import com.man.backend.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class CardService {

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
        return cardRepository.findByIdAndUserOpenid(id, normalizedOpenid)
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
            return false;
        }
        cardRepository.delete(cardOptional.get());
        return true;
    }

    private void validateCardRequest(CardRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request body is required");
        }
        requireNonBlank(request.getOpenid(), "openid is required");
        requireNonBlank(request.getName(), "name is required");
        if (request.getPurchaseDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "purchaseDate is required");
        }
        if (request.getAmount() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount is required");
        }
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }
}
