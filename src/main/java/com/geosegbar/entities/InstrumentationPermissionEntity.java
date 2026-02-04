package com.geosegbar.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name = "instrumentation_permissions",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"user_id"})
        }
)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class InstrumentationPermissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private UserEntity user;

    @Column(name = "view_graphs", nullable = false)
    private Boolean viewGraphs = false;

    @Column(name = "edit_graphs_local", nullable = false)
    private Boolean editGraphsLocal = false;

    @Column(name = "edit_graphs_default", nullable = false)
    private Boolean editGraphsDefault = false;

    @Column(name = "view_read", nullable = false)
    private Boolean viewRead = false;

    @Column(name = "edit_read", nullable = false)
    private Boolean editRead = false;

    @Column(name = "view_sections", nullable = false)
    private Boolean viewSections = false;

    @Column(name = "edit_sections", nullable = false)
    private Boolean editSections = false;

    @Column(name = "view_instruments", nullable = true, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean viewInstruments = false;

    @Column(name = "edit_instruments", nullable = true, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean editInstruments = false;
}
