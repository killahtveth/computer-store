package ru.ulstu.computerstore.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

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

    @OneToMany(mappedBy = "computer", cascade = CascadeType.ALL)
    private List<ComputerPriceHistory> priceHistory;
}