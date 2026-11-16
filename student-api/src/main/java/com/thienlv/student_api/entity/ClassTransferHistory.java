package com.thienlv.student_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "class_transfer_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassTransferHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_class_id", nullable = false)
    private SchoolClass fromClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_class_id", nullable = false)
    private SchoolClass toClass;

    @Column(name = "transferred_at", nullable = false)
    private LocalDateTime transferredAt;
}
