package com.example.p0znyaks.dto.binance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BinanceErrorResponse(Integer code, String msg) {

    public static final int UNKNOWN_CODE = -1;

    public int codeOrUnknown() {
        return code != null ? code : UNKNOWN_CODE;
    }
}