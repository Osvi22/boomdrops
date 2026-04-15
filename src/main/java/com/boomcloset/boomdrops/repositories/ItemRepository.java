package com.boomcloset.boomdrops.repositories;

import com.boomcloset.boomdrops.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {
}