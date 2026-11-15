package com.studyai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Configuration de l'audit JPA (pour les annotations @CreatedDate et @LastModifiedDate).
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {}
