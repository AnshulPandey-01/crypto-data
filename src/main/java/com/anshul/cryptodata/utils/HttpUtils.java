package com.anshul.cryptodata.utils;

import java.util.Collections;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

public class HttpUtils {

	private static final String API_HOST = "coinranking1.p.rapidapi.com";
	private static final String API_KEY = "4b7eb18a29mshbf6c6cb07a042ecp164301jsn451cec351e4d";
	
	public static HttpEntity<String> getHttpEntity() {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		headers.set("X-RapidAPI-Host", API_HOST);
		headers.set("X-RapidAPI-Key", API_KEY);
		return new HttpEntity<String>(null, headers);
	}
	
}
