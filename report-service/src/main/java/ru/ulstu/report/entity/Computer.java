package ru.ulstu.report.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "computers")
public class Computer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String model;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "status_id", nullable = false)
    private ComputerStatus status;

    @OneToMany(mappedBy = "computer", fetch = FetchType.LAZY)
    private java.util.List<ComputerPriceHistory> priceHistory;
}