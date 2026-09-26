package ru.ulstu.report.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.ulstu.report.entity.Computer;

import java.util.List;

public interface ComputerRepository extends JpaRepository<Computer, Long> {
    List<Computer> findByStatus_Code(String code);
    long countByStatus_Code(String code);
}