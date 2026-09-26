package ru.ulstu.computerstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.ulstu.computerstore.entity.ComputerStatus;

import java.util.Optional;

public interface ComputerStatusRepository extends JpaRepository<ComputerStatus, Long> {
    Optional<ComputerStatus> findByCode(String code);
}