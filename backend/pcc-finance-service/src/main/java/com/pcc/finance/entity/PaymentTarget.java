package com.pcc.finance.entity;

import com.pcc.finance.enums.TargetType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "payment_targets",
        uniqueConstraints = @UniqueConstraint(columnNames = {"targetType", "externalId"}))
public class PaymentTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TargetType targetType;

    @Column(nullable = false)
    private Long externalId;

    @Column(nullable = false, length = 60)
    private String externalReference;

    @Column(length = 60)
    private String parentReference;

    private String description;

    @Column(nullable = false, precision = 18)
    private BigDecimal totalAmount;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String snapshot;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
