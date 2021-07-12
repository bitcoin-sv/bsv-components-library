package com.nchain.jcl.net.protocol.handlers.common;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 11/07/2021
 */
public class SimpleBlockingMap<K, V> implements Map<K,V>{

    private Logger logger = LoggerFactory.getLogger(SimpleBlockingMap.class);

    private Map<K, V> map = new ConcurrentHashMap<>();
    private Set<K> mapLookups = Collections.synchronizedSet(new HashSet<>());

    public SimpleBlockingMap() {
    }

    /**
     * If the element does not exist, waits the given duration and tries again
     * @param key
     * @param timeout
     * @return
     */
    public V take(K key, Duration timeout) throws InterruptedException {
        if(!map.containsKey(key) && !mapLookups.contains(key)){
                try {
                    Thread.sleep(timeout.toMillis());
                    mapLookups.add(key);
                } catch (InterruptedException e) {
                    throw e;
                }
        }

        return map.get(key);
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsKey(value);
    }

    @Override
    public V get(Object key) {
        return map.get(key);
    }

    @Override
    public V put(K key, V value) {
        return map.put(key, value);
    }

    @Override
    public V remove(Object key) {
        mapLookups.remove(key);

        return map.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        map.putAll(m);
    }

    @Override
    public void clear() {
        mapLookups.clear();
        map.clear();
    }

    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<V> values() {
        return map.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return map.entrySet();
    }
}
