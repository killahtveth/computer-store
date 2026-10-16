package ru.ulstu.saga.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "saga_instances")
public class SagaInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Уникальный идентификатор саги (UUID) */
    @Column(name = "saga_id", nullable = false, unique = true)
    private String sagaId;

    /** requestId, сгенерированный core-service */
    @Column(name = "request_id", nullable = false)
    private String requestId;

    /** STARTED, STEP_1_IN_PROGRESS, STEP_1_DONE, STEP_2_IN_PROGRESS, STEP_2_DONE,
     *  COMPLETED, COMPENSATING, COMPENSATED, FAILED */
    @Column(nullable = false)
    private String status;

    /** текущий шаг: REPORT_UPLOAD, REPORT_DELETE, NOTIFY */
    @Column(name = "current_step")
    private String currentStep;

    /** сериализованный payload (JSON) — данные для отчёта */
    @Column(columnDefinition = "TEXT")
    private String payload;

    /** имя файла в MinIO (заполняется после шага 1) */
    @Column(name = "object_name")
    private String objectName;

    /** причина провала */
    @Column(columnDefinition = "TEXT")
    private String error;

    @Column(name = "trace_id")
    private String traceId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}