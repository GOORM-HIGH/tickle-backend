package com.profect.tickle.domain.performance.repository;

import com.profect.tickle.domain.performance.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenreRepository extends JpaRepository<Genre,Long> {
}
