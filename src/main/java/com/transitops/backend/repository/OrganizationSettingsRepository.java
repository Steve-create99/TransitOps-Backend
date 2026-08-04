package com.transitops.backend.repository;

import com.transitops.backend.entity.OrganizationSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationSettingsRepository extends JpaRepository<OrganizationSettings, Long> {
}
