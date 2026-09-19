package ru.ulstu.computerstore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.ulstu.computerstore.dto.ComputerRequest;
import ru.ulstu.computerstore.dto.ReportResponse;
import ru.ulstu.computerstore.entity.Computer;
import ru.ulstu.computerstore.repository.ComputerRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ComputerService {

    private final ComputerRepository repository;

    public Computer addComputer(ComputerRequest request) {
        Computer computer = new Computer();
        computer.setModel(request.model());
        computer.setPrice(request.price());
        computer.setStatus("AVAILABLE");
        return repository.save(computer);
    }

    public List<Computer> getAllComputers() {
        return repository.findAll();
    }

    public Computer getComputerById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Компьютер с id " + id + " не найден"));
    }

    public Computer updateComputer(Long id, ComputerRequest request) {
        Computer computer = getComputerById(id);
        computer.setModel(request.model());
        computer.setPrice(request.price());
        return repository.save(computer);
    }

    public void deleteComputer(Long id) {
        Computer computer = getComputerById(id);
        repository.delete(computer);
    }

    public Computer sellComputer(Long id) {
        Computer computer = getComputerById(id);
        if ("SOLD".equals(computer.getStatus())) {
            throw new RuntimeException("Компьютер уже продан");
        }
        computer.setStatus("SOLD");
        return repository.save(computer);
    }

    public ReportResponse getReport() {
        long available = repository.countByStatus("AVAILABLE");
        long sold = repository.countByStatus("SOLD");
        return new ReportResponse(available, sold);
    }
}