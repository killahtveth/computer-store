package ru.ulstu.computerstore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ulstu.computerstore.dto.ComputerRequest;
import ru.ulstu.computerstore.dto.ComputerResponse;
import ru.ulstu.computerstore.dto.ReportResponse;
import ru.ulstu.computerstore.entity.Computer;
import ru.ulstu.computerstore.entity.ComputerPriceHistory;
import ru.ulstu.computerstore.entity.ComputerStatus;
import ru.ulstu.computerstore.repository.ComputerPriceHistoryRepository;
import ru.ulstu.computerstore.repository.ComputerRepository;
import ru.ulstu.computerstore.repository.ComputerStatusRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ComputerService {

    private final ComputerRepository computerRepository;
    private final ComputerStatusRepository statusRepository;
    private final ComputerPriceHistoryRepository priceHistoryRepository;

    @Transactional
    public ComputerResponse addComputer(ComputerRequest request) {
    ComputerStatus available = statusRepository.findByCode("AVAILABLE")
            .orElseThrow(() -> new IllegalStateException("Статус AVAILABLE не найден"));

    Computer computer = new Computer();
    computer.setModel(request.model());
    computer.setStatus(available);
    Computer saved = computerRepository.save(computer);

    ComputerPriceHistory history = new ComputerPriceHistory();
    history.setComputer(saved);
    history.setPrice(request.price());
    history.setValidFrom(LocalDateTime.now());
    history.setValidTo(null);
    ComputerPriceHistory savedHistory = priceHistoryRepository.save(history);

    // Собираем DTO вручную — не перечитываем сущность из БД
    return new ComputerResponse(
            saved.getId(),
            saved.getModel(),
            available.getCode(),
            available.getName(),
            List.of(new ComputerResponse.PriceHistoryDto(
                    savedHistory.getId(),
                    savedHistory.getPrice(),
                    savedHistory.getValidFrom().toString(),
                    null
            ))
    );
}

    @Transactional(readOnly = true)
    public List<ComputerResponse> getAllComputers() {
        return computerRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ComputerResponse getComputerById(Long id) {
        return toDto(getComputerEntity(id));
    }

    @Transactional
    public ComputerResponse updateComputer(Long id, ComputerRequest request) {
        Computer computer = getComputerEntity(id);
        computer.setModel(request.model());
        changePriceInternal(computer, request.price());
        computerRepository.save(computer);
        return toDto(computer);
    }

    @Transactional
    public void deleteComputer(Long id) {
        Computer computer = getComputerEntity(id);
        computerRepository.delete(computer);
    }

    @Transactional
    public ComputerResponse sellComputer(Long id) {
        Computer computer = getComputerEntity(id);
        if ("SOLD".equals(computer.getStatus().getCode())) {
            throw new RuntimeException("Компьютер уже продан");
        }
        ComputerStatus sold = statusRepository.findByCode("SOLD")
                .orElseThrow(() -> new IllegalStateException("Статус SOLD не найден"));
        computer.setStatus(sold);
        computerRepository.save(computer);
        return toDto(computer);
    }

    @Transactional
    public ComputerResponse changePrice(Long id, BigDecimal newPrice) {
        Computer computer = getComputerEntity(id);
        changePriceInternal(computer, newPrice);
        return toDto(computer);
    }

    @Transactional(readOnly = true)
    public ReportResponse getReport() {
        long available = computerRepository.countByStatus_Code("AVAILABLE");
        long sold = computerRepository.countByStatus_Code("SOLD");
        return new ReportResponse(available, sold);
    }

    private Computer getComputerEntity(Long id) {
        return computerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Компьютер с id " + id + " не найден"));
    }

    private void changePriceInternal(Computer computer, BigDecimal newPrice) {
        LocalDateTime now = LocalDateTime.now();

        priceHistoryRepository.findByComputerIdAndValidToIsNull(computer.getId())
                .ifPresent(current -> {
                    current.setValidTo(now);
                    priceHistoryRepository.save(current);
                });

        ComputerPriceHistory newRecord = new ComputerPriceHistory();
        newRecord.setComputer(computer);
        newRecord.setPrice(newPrice);
        newRecord.setValidFrom(now);
        newRecord.setValidTo(null);
        priceHistoryRepository.save(newRecord);
    }

    private ComputerResponse toDto(Computer c) {
    List<ComputerPriceHistory> history = c.getPriceHistory() == null 
            ? List.of() 
            : c.getPriceHistory();
    
    List<ComputerResponse.PriceHistoryDto> historyDtos = history.stream()
            .map(h -> new ComputerResponse.PriceHistoryDto(
                    h.getId(),
                    h.getPrice(),
                    h.getValidFrom() != null ? h.getValidFrom().toString() : null,
                    h.getValidTo() != null ? h.getValidTo().toString() : null))
            .toList();

    return new ComputerResponse(
            c.getId(),
            c.getModel(),
            c.getStatus().getCode(),
            c.getStatus().getName(),
            historyDtos
    );
}
}