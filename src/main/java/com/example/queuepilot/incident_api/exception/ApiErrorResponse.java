package com.example.queuepilot.incident_api.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ApiErrorResponse{
    private int status;
    private String error;
    private String message;
    private LocalDateTime time;
}
