package com.boomcloset.boomdrops.repositories;

import com.boomcloset.boomdrops.model.Drop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DropRepository extends JpaRepository<Drop, Long> {
    List<Drop> findByNombreContainingIgnoreCase(String nombre);
}