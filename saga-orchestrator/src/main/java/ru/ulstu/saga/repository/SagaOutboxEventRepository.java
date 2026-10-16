package ru.ulstu.saga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.ulstu.saga.entity.SagaOutboxEvent;

import java.util.List;

public interface SagaOutboxEventRepository extends JpaRepository<SagaOutboxEvent, Long> {
    List<SagaOutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(String status);
}