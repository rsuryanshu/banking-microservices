package com.banking.common_config.dto;

import lombok.*;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {
    private String timestamp;
    private int status;
    private String error;
    private String message;
    private String code;
    private String path;
    private List<Map<String, String>> fieldErrors;
}