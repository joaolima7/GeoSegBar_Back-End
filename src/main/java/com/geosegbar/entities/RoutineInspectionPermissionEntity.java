package com.geosegbar.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "routine_inspection_permissions", 
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id"})
    }
)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RoutineInspectionPermissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;
    
    @Column(name = "is_fill_web", nullable = false)
    private Boolean isFillWeb = false;
    
    @Column(name = "is_fill_mobile", nullable = false)
    private Boolean isFillMobile = false;
}
