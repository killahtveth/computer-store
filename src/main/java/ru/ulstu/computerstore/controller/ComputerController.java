package ru.ulstu.computerstore.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.ulstu.computerstore.dto.ComputerRequest;
import ru.ulstu.computerstore.dto.ReportResponse;
import ru.ulstu.computerstore.entity.Computer;
import ru.ulstu.computerstore.service.ComputerService;

import java.util.List;

@RestController
@RequestMapping("/api/computers")
@RequiredArgsConstructor
@Tag(name = "Учет компьютеров", description = "CRUD операции и бизнес-логика магазина")
public class ComputerController {

    private final ComputerService service;

    @PostMapping
    @Operation(summary = "Создать (принять новый компьютер)")
    public ResponseEntity<Computer> addComputer(@RequestBody ComputerRequest request) {
        return ResponseEntity.ok(service.addComputer(request));
    }

    @GetMapping
    @Operation(summary = "Получить список всех компьютеров")
    public ResponseEntity<List<Computer>> getAllComputers() {
        return ResponseEntity.ok(service.getAllComputers());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить компьютер по ID")
    public ResponseEntity<Computer> getComputerById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getComputerById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить информацию о компьютере")
    public ResponseEntity<Computer> updateComputer(@PathVariable Long id, @RequestBody ComputerRequest request) {
        return ResponseEntity.ok(service.updateComputer(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить компьютер из базы")
    public ResponseEntity<Void> deleteComputer(@PathVariable Long id) {
        service.deleteComputer(id);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/{id}/sell")
    @Operation(summary = "Оформить продажу компьютера (смена статуса)")
    public ResponseEntity<Computer> sellComputer(@PathVariable Long id) {
        return ResponseEntity.ok(service.sellComputer(id));
    }

    @GetMapping("/report")
    @Operation(summary = "Сформировать отчет (в наличии / продано)")
    public ResponseEntity<ReportResponse> getReport() {
        return ResponseEntity.ok(service.getReport());
    }
}