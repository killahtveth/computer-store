package ru.ulstu.computerstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.ulstu.computerstore.entity.Computer;

public interface ComputerRepository extends JpaRepository<Computer, Long> {
    long countByStatus(String status);
}