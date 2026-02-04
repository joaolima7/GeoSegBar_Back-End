package com.geosegbar.infra.status.persistence.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.common.enums.StatusEnum;
import com.geosegbar.entities.StatusEntity;

@Repository
public interface StatusRepository extends JpaRepository<StatusEntity, Long> {

    Optional<StatusEntity> findByStatus(StatusEnum status);
}
