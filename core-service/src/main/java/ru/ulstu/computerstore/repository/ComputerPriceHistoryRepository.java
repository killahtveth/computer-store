package ru.ulstu.computerstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.ulstu.computerstore.entity.ComputerPriceHistory;

import java.util.List;
import java.util.Optional;

public interface ComputerPriceHistoryRepository extends JpaRepository<ComputerPriceHistory, Long> {
    Optional<ComputerPriceHistory> findByComputerIdAndValidToIsNull(Long computerId);
    List<ComputerPriceHistory> findByComputerIdOrderByValidFromDesc(Long computerId);
}