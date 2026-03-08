package com.man.backend.card.repository;

import com.man.backend.card.model.Card;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {

    List<Card> findByUserOpenidOrderByIdAsc(String openid);

    Optional<Card> findByIdAndUserOpenid(Long id, String openid);
}
