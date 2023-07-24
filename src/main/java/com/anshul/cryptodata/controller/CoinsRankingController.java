package com.anshul.cryptodata.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.anshul.cryptodata.model.CoinInfo;
import com.anshul.cryptodata.model.HistoryData;
import com.anshul.cryptodata.service.CoinsDataService;
import com.anshul.cryptodata.utils.Utility;

import io.github.dengliming.redismodule.redistimeseries.Sample;

@RestController
@RequestMapping("/coins")
public class CoinsRankingController {

	@Autowired
	private CoinsDataService coinsDataService;
	
	@GetMapping
	public ResponseEntity<List<CoinInfo>> fetchAllCoins() {
		return ResponseEntity.ok().body(coinsDataService.fetchAllCoinsFromRedisJSON());
	}
	
	@GetMapping("/{symbol}/timeperiod/{timePeriod}")
	public ResponseEntity<List<HistoryData>> fetchCoinHistoryPerTimePeriod(@PathVariable String symbol, @PathVariable String timePeriod) {
		List<Sample.Value> coinsTSData = coinsDataService.fetchCoinHistoryPerTimePeriodFromRedisTS(symbol, timePeriod);
		List<HistoryData> coinHistory = new ArrayList<>();
		coinsTSData.forEach(value -> 
			coinHistory.add(new HistoryData(Utility.convertUnixTimeToDate(value.getTimestamp()), Utility.round(value.getValue(), 2)))
		);
		return ResponseEntity.ok().body(coinHistory);
	}
	
}
