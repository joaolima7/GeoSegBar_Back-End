package com.geosegbar.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "anomaly_photos")
public class AnomalyPhotoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Informe a anomalia a qual a foto pertence!")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anomaly_id", nullable = false)
    @JsonBackReference(value = "anomaly-photos")
    private AnomalyEntity anomaly;

    @Column(name = "image_path", nullable = false)
    private String imagePath;

    @Column(name = "dam_id")
    private Long damId;
}
