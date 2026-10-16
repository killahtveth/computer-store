package ru.ulstu.saga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.ulstu.saga.entity.SagaInstance;

import java.util.Optional;

public interface SagaInstanceRepository extends JpaRepository<SagaInstance, Long> {
    Optional<SagaInstance> findBySagaId(String sagaId);
    Optional<SagaInstance> findByRequestId(String requestId);
}