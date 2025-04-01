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
@Table(name = "attributions_permissions", 
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id"})
    }
)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AttributionsPermissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;
    
    @Column(name = "edit_user", nullable = false)
    private Boolean editUser = false;
    
    @Column(name = "edit_dam", nullable = false)
    private Boolean editDam = false;
    
    @Column(name = "edit_geral_data", nullable = false)
    private Boolean editGeralData = false;
}
