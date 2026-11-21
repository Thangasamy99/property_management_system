package com.pcc.property.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "rental_agreement_terms")
public class RentalAgreementTerm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agreement_id")
    private RentalAgreement agreement;

    @Column(nullable = false)
    private Integer termOrder;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
}
