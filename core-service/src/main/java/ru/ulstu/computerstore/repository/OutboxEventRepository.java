package ru.ulstu.computerstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.ulstu.computerstore.entity.OutboxEvent;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(String status);
}