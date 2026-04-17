package com.geosegbar.infra.pae.persistence.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.PAEProtectionElementEntity;

@Repository
public interface PAEProtectionElementRepository extends JpaRepository<PAEProtectionElementEntity, Long> {

    List<PAEProtectionElementEntity> findByPaeId(Long paeId);
}
