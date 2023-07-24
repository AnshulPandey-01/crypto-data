package com.anshul.cryptodata.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.anshul.cryptodata.model.CoinData;
import com.anshul.cryptodata.model.CoinInfo;
import com.anshul.cryptodata.model.CoinPriceHistory;
import com.anshul.cryptodata.model.CoinPriceHistoryExchangeRate;
import com.anshul.cryptodata.model.Coins;
import com.anshul.cryptodata.utils.HttpUtils;

import io.github.dengliming.redismodule.redisjson.RedisJSON;
import io.github.dengliming.redismodule.redisjson.args.GetArgs;
import io.github.dengliming.redismodule.redisjson.args.SetArgs;
import io.github.dengliming.redismodule.redisjson.utils.GsonUtils;
import io.github.dengliming.redismodule.redistimeseries.DuplicatePolicy;
import io.github.dengliming.redismodule.redistimeseries.RedisTimeSeries;
import io.github.dengliming.redismodule.redistimeseries.Sample;
import io.github.dengliming.redismodule.redistimeseries.Sample.Value;
import io.github.dengliming.redismodule.redistimeseries.TimeSeriesOptions;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CoinsDataService {
	
	public static final String GET_COINS_API = "https://coinranking1.p.rapidapi.com/coins?referenceCurrencyUuid=yhjMzLPhuIDl&timePeriod=24h&tiers%5B0%5D=1&orderBy=marketCap&orderDirection=desc&limit=50&offset=0";
	public static final String GET_COINS_HISTORY_API = "https://coinranking1.p.rapidapi.com/coin/";
	public static final String COINS_HISTORY_TIME_PERIOD_PARAM = "/history?timePeriod=";
	public static final List<String> TIME_PERIODS = List.of("24h", "7d", "30d", "3m", "1y", "3y", "5y");
	public static final String REDIS_COINS_KEY = "coins";
	
	@Autowired
	private RestTemplate restTemplate;
	@Autowired
	private RedisJSON redisJSON;
	@Autowired
	private RedisTimeSeries redisTimeSeries;
	
	public void fetchCoins() {
		log.info("Inside fetchCoins()");
		ResponseEntity<Coins> coinsEntity = restTemplate.exchange(GET_COINS_API, HttpMethod.GET, HttpUtils.getHttpEntity(), Coins.class);
		storeCoinsToRedisJSON(coinsEntity.getBody());
	}

	private void storeCoinsToRedisJSON(Coins coins) {
		redisJSON.set(REDIS_COINS_KEY, SetArgs.Builder.create(".", GsonUtils.toJson(coins)));
	}
	
	public void fetchCoinsHistory() {
		log.info("Inside fetchCoinsHistory()");
		
		List<CoinInfo> allCoins = getAllCoinsFromRedisJSON();
		allCoins.forEach(coinInfo -> {
			TIME_PERIODS.forEach(s -> {
				fetchCoinHistoryForTimePeriod(coinInfo, s);
			});
		});
		
		log.info("All coins hostory fetched successfully");
	}

	private List<CoinInfo> getAllCoinsFromRedisJSON() {
		CoinData coinData = redisJSON.get(REDIS_COINS_KEY, CoinData.class, new GetArgs().path(".data").indent("\t").newLine("\n").space(" "));
		return coinData.getCoins();
	}
	
	private void fetchCoinHistoryForTimePeriod(CoinInfo coinInfo, String timePeriod) {
		log.info("Fetching coin history of {} for time period {}", coinInfo.getName(), timePeriod);
		
		String url = GET_COINS_HISTORY_API + coinInfo.getUuid() + COINS_HISTORY_TIME_PERIOD_PARAM + timePeriod;
		ResponseEntity<CoinPriceHistory> coinPriceHistoryEntity = restTemplate.exchange(url, HttpMethod.GET, HttpUtils.getHttpEntity(), CoinPriceHistory.class);
		
		log.info("Data fetched from history API for {} coin for time period {}", coinInfo.getName(), timePeriod);
		
		storeCoinHistoryToRedisTS(coinPriceHistoryEntity.getBody(), coinInfo.getSymbol(), timePeriod);
	}

	private void storeCoinHistoryToRedisTS(CoinPriceHistory coinPriceHistory, String symbol, String timePeriod) {
		log.info("Storing coin history of {} for time period {} into RedisTS", symbol, timePeriod);
		
		List<CoinPriceHistoryExchangeRate> coinExchangeRate = coinPriceHistory.getData().getHistory();
		coinExchangeRate.stream()
			.filter(cer -> cer.getPrice() != null && cer.getTimestamp() != null)
			.forEach(cer -> {
				redisTimeSeries.add(
						new Sample(symbol + ":" + timePeriod, Sample.Value.of(Long.valueOf(cer.getTimestamp()), Double.valueOf(cer.getPrice()))), 
						new TimeSeriesOptions().unCompressed().duplicatePolicy(DuplicatePolicy.LAST)
					);
			});
		
		log.info("Completed storing coin history of {} for time period {} into RedisTS", symbol, timePeriod);
	}

	public List<CoinInfo> fetchAllCoinsFromRedisJSON() {
		return getAllCoinsFromRedisJSON();
	}

	public List<Sample.Value> fetchCoinHistoryPerTimePeriodFromRedisTS(String symbol, String timePeriod) {
		Map<String, Object> tsInfo = fetchTSInfoForSymbol(symbol, timePeriod);
		Long firstTimestamp = Long.valueOf(tsInfo.get("firstTimestamp").toString());
		Long lastTimestamp = Long.valueOf(tsInfo.get("lastTimestamp").toString());
		
		return fetchTSDataForCoin(symbol, timePeriod, firstTimestamp, lastTimestamp);
	}

	private Map<String, Object> fetchTSInfoForSymbol(String symbol, String timePeriod) {
		return redisTimeSeries.info(symbol + ":" + timePeriod);
	}
	
	private List<Value> fetchTSDataForCoin(String symbol, String timePeriod, Long firstTimestamp, Long lastTimestamp) {
		String key = symbol + ":" + timePeriod;
		return redisTimeSeries.range(key, firstTimestamp, lastTimestamp);
	}

}
