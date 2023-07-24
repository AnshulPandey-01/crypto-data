package com.anshul.cryptodata.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HistoryData {
    private String timestamp;
    private double value;
}
