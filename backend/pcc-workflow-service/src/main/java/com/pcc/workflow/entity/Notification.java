package com.pcc.workflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter @Setter @Entity @Table(name = "notifications")
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long recipientId;
    @Column(length = 40) private String recipientRole;
    @Column(nullable = false, length = 30) private String type;
    @Column(nullable = false, length = 200) private String title;
    @Column(length = 500) private String message;
    @Column(length = 20) private String workflowRef;
    @Column(nullable = false) private boolean readFlag = false;
    @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();
}
