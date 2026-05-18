package com.processmonitor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    private String error;
    private String code;
    private LocalDateTime timestamp;
    private String correlationId;

    public static ErrorResponse of(String error, String code, String correlationId) {
        return ErrorResponse.builder()
                .error(error)
                .code(code)
                .timestamp(LocalDateTime.now())
                .correlationId(correlationId)
                .build();
    }
}
