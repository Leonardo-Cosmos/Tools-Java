package com.lanmessager.concurrent;

public class Report<K> {
	private K key;

	public Report(K key) {
		this.key = key;
	}
	
	public K getKey() {
		return key;
	}
}
