package ru.ulstu.report.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ulstu.report.dto.ComputerReportItem;
import ru.ulstu.report.dto.StatusReport;
import ru.ulstu.report.entity.Computer;
import ru.ulstu.report.entity.ComputerPriceHistory;
import ru.ulstu.report.repository.ComputerPriceHistoryRepository;
import ru.ulstu.report.repository.ComputerRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ComputerRepository computerRepository;
    private final ComputerPriceHistoryRepository priceHistoryRepository;

    /**
     * Список всех компьютеров с их текущей ценой.
     */
    @Transactional(readOnly = true)
    public List<ComputerReportItem> getAllComputersReport() {
        List<Computer> computers = computerRepository.findAll();
        List<ComputerPriceHistory> currentPrices = priceHistoryRepository.findByValidToIsNull();

        return computers.stream()
                .map(c -> {
                    BigDecimal currentPrice = currentPrices.stream()
                            .filter(h -> h.getComputer().getId().equals(c.getId()))
                            .map(ComputerPriceHistory::getPrice)
                            .findFirst()
                            .orElse(null);

                    return new ComputerReportItem(
                            c.getId(),
                            c.getModel(),
                            c.getStatus().getCode(),
                            c.getStatus().getName(),
                            currentPrice
                    );
                })
                .toList();
    }

    /**
     * Отчёт по количеству компьютеров в каждом статусе.
     */
    @Transactional(readOnly = true)
    public StatusReport getStatusReport() {
        long available = computerRepository.countByStatus_Code("AVAILABLE");
        long sold = computerRepository.countByStatus_Code("SOLD");
        long total = computerRepository.count();
        return new StatusReport(available, sold, total);
    }

    /**
     * История цен конкретного компьютера.
     */
    @Transactional(readOnly = true)
    public List<ComputerPriceHistory> getPriceHistory(Long computerId) {
        return priceHistoryRepository.findByComputerIdOrderByValidFromDesc(computerId);
    }
}