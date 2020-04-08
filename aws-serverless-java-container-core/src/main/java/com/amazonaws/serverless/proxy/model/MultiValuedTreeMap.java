/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.amazonaws.serverless.proxy.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.ws.rs.core.MultivaluedMap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Simple implementation of a multi valued tree map to use for case-insensitive headers
 *
 * @param <Key> The type for the map key
 * @param <Value> The type for the map values
 */
public class MultiValuedTreeMap<Key, Value> implements MultivaluedMap<Key, Value>, Serializable, Cloneable {

    private static final long serialVersionUID = 42L;

    private final Map<Key, List<Value>> map;


    public MultiValuedTreeMap() {
        map = new TreeMap<>();
    }

    public MultiValuedTreeMap(Comparator<Key> comparator) {
        map = new TreeMap<>(comparator);
    }

    @Override
    public void add(Key key, Value value) {
        List<Value> values = findKey(key);
        values.add(value);
    }

    @Override
    public Value getFirst(Key key) {
        List<Value> values = get(key);
        if (values == null || values.size() == 0) {
            return null;
        }
        return values.get(0);
    }

    @Override
    public void putSingle(Key key, Value value) {
        List<Value> values = findKey(key);
        values.clear();
        values.add(value);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public Set<Entry<Key, List<Value>>> entrySet() {
        return map.entrySet();
    }

    public boolean equals(Object o) {
        return map.equals(o);
    }

    @Override
    public List<Value> get(Object key) {
        return map.get(key);
    }

    public int hashCode() {
        return map.hashCode();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public Set<Key> keySet() {
        return map.keySet();
    }

    @Override
    public List<Value> put(Key key, List<Value> value) {
        return map.put(key, value);
    }

    @Override
    public void putAll(Map<? extends Key, ? extends List<Value>> t) {
        map.putAll(t);
    }

    @Override
    public List<Value> remove(Object key) {
        return map.remove(key);
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public Collection<List<Value>> values() {
        return map.values();
    }

    @Override
    public void addAll(Key key, Value... newValues) {
        for (Value value : newValues) {
            add(key, value);
        }
    }

    @Override
    public void addAll(Key key, List<Value> valueList) {
        for (Value value : valueList) {
            add(key, value);
        }
    }

    @Override
    public void addFirst(Key key, Value value) {
        List<Value> values = get(key);
        if (values == null) {
            add(key, value);
            return;
        } else {
            values.add(0, value);
        }
    }

    @Override
    public boolean equalsIgnoreValueOrder(MultivaluedMap<Key, Value> vmap) {
        if (this == vmap) {
            return true;
        }
        if (!keySet().equals(vmap.keySet())) {
            return false;
        }
        for (Map.Entry<Key, List<Value>> e : entrySet()) {
            List<Value> olist = vmap.get(e.getKey());
            if (e.getValue().size() != olist.size()) {
                return false;
            }
            for (Value v : e.getValue()) {
                if (!olist.contains(v)) {
                    return false;
                }
            }
        }
        return true;
    }

    private List<Value> findKey(Key key) {
        List<Value> values = this.get(key);
        if (values == null) {
            values = new ArrayList<>();
            put(key, values);
        }
        return values;
    }

    @Override
    @SuppressFBWarnings("CN_IDIOM_NO_SUPER_CALL")
    public MultiValuedTreeMap<Key, Value> clone() {
        MultiValuedTreeMap<Key, Value> clone = new MultiValuedTreeMap<>();
        for (Key key : keySet()) {
            List<Value> value = get(key);
            List<Value> newValue = new ArrayList<>(value);
            clone.put(key, newValue);
        }
        return clone;
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        String delim = ",";
        for (Object name : keySet()) {
            for (Object value : get(name)) {
                result.append(delim);
                if (name == null) {
                    result.append("null"); //$NON-NLS-1$
                } else {
                    result.append(name.toString());
                }
                if (value != null) {
                    result.append('=');
                    result.append(value.toString());
                }
            }
        }
        return "[" + result.toString() + "]";
    }
}
