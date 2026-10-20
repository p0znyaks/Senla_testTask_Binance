package com.example.p0znyaks.dto.binance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Error payload returned by Binance, for example {@code {"code": -1121, "msg": "Invalid symbol."}}.
 *
 * <p>Unknown fields are ignored so that Binance can add attributes without breaking the poll.
 *
 * @param code numeric error code, {@code null} when Binance did not send one
 * @param msg  human readable error message, {@code null} when Binance did not send one
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BinanceErrorResponse(Integer code, String msg) {

    public static final int UNKNOWN_CODE = -1;

    /**
     * @return the numeric code, or {@link #UNKNOWN_CODE} if the payload carries none
     */
    public int codeOrUnknown() {
        return code != null ? code : UNKNOWN_CODE;
    }
}