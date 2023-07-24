package com.anshul.cryptodata.config;

import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.dengliming.redismodule.redisjson.RedisJSON;
import io.github.dengliming.redismodule.redisjson.client.RedisJSONClient;
import io.github.dengliming.redismodule.redistimeseries.RedisTimeSeries;
import io.github.dengliming.redismodule.redistimeseries.client.RedisTimeSeriesClient;

@Configuration
public class RedisConfig {
	
	private static final String REDIS_URL = "redis://127.0.0.1:6379";
	
	@Bean
	public Config config() {
		Config config = new Config();
		config.useSingleServer().setAddress(REDIS_URL);
		return config;
	}
	
	@Bean
	public RedisTimeSeriesClient redisTimeSeriesClient(Config config) {
		return new RedisTimeSeriesClient(config);
	}
	
	@Bean
	public RedisTimeSeries redisTimeSeries(RedisTimeSeriesClient redisTimeSeriesClient) {
		return redisTimeSeriesClient.getRedisTimeSeries();
	}
	
	@Bean
	public RedisJSONClient redisJSONClient(Config config) {
		return new RedisJSONClient(config);
	}
	
	@Bean
	public RedisJSON redisJSON(RedisJSONClient redisJSONClient) {
		return redisJSONClient.getRedisJSON();
	}

}
