/*
 * alert-database
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.alert.database.job.jira.server.custom_field;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JiraServerJobCustomFieldRepository extends JpaRepository<JiraServerJobCustomFieldEntity, JiraServerJobCustomFieldPK> {
    List<JiraServerJobCustomFieldEntity> findByJobId(UUID jobId);

    @Query("DELETE FROM JiraServerJobCustomFieldEntity entity"
               + " WHERE entity.jobId = :jobId"
    )
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    void bulkDeleteByJobId(@Param("jobId") UUID jobId);

}
