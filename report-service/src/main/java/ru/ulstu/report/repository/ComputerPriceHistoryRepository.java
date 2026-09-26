package ru.ulstu.report.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.ulstu.report.entity.ComputerPriceHistory;

import java.util.List;

public interface ComputerPriceHistoryRepository extends JpaRepository<ComputerPriceHistory, Long> {
    List<ComputerPriceHistory> findByComputerIdOrderByValidFromDesc(Long computerId);
    List<ComputerPriceHistory> findByValidToIsNull();
}