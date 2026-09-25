package ru.ulstu.computerstore.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.ulstu.computerstore.dto.ComputerRequest;
import ru.ulstu.computerstore.dto.ComputerResponse;
import ru.ulstu.computerstore.dto.ReportResponse;
import ru.ulstu.computerstore.service.ComputerService;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/computers")
@RequiredArgsConstructor
public class ComputerController {

    private final ComputerService service;

    @PostMapping
    public ResponseEntity<ComputerResponse> addComputer(@RequestBody ComputerRequest request) {
        return ResponseEntity.ok(service.addComputer(request));
    }

    @GetMapping
    public ResponseEntity<List<ComputerResponse>> getAllComputers() {
        return ResponseEntity.ok(service.getAllComputers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComputerResponse> getComputerById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getComputerById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ComputerResponse> updateComputer(@PathVariable Long id,
                                                           @RequestBody ComputerRequest request) {
        return ResponseEntity.ok(service.updateComputer(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComputer(@PathVariable Long id) {
        service.deleteComputer(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/sell")
    public ResponseEntity<ComputerResponse> sellComputer(@PathVariable Long id) {
        return ResponseEntity.ok(service.sellComputer(id));
    }

    @PutMapping("/{id}/price")
    public ResponseEntity<ComputerResponse> changePrice(@PathVariable Long id,
                                                        @RequestBody BigDecimal newPrice) {
        return ResponseEntity.ok(service.changePrice(id, newPrice));
    }

    @GetMapping("/report")
    public ResponseEntity<ReportResponse> getReport() {
        return ResponseEntity.ok(service.getReport());
    }
}