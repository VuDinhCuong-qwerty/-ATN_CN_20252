package com.iam.app.dto.response;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ClientListResponse {

    private final Long id;
    private final String clientId;
    private final String name;
    private final String clientType;
    private final Integer enabled;
    private final Long appId;
    private final String appName;
    private final String grantTypes;
    private final LocalDateTime createdAt;

    public ClientListResponse(Object[] row) {
        this.id = toLong(row[0]);
        this.clientId = (String) row[1];
        this.name = (String) row[2];
        this.clientType = (String) row[3];
        this.enabled = toInteger(row[4]);
        this.appId = row[5] != null ? toLong(row[5]) : null;
        this.createdAt = row[6] != null ? ((java.sql.Timestamp) row[6]).toLocalDateTime() : null;
        this.grantTypes = (String) row[7];
        this.appName = (String) row[8];
    }

    private static Long toLong(Object val) {
        if (val == null) return null;
        return ((Number) val).longValue();
    }

    private static Integer toInteger(Object val) {
        if (val == null) return null;
        return ((Number) val).intValue();
    }
}
