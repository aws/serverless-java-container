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

    public void add(Key key, Value value) {
        List<Value> values = findKey(key);
        values.add(value);
    }

    public Value getFirst(Key key) {
        List<Value> values = get(key);
        if (values == null || values.size() == 0) {
            return null;
        }
        return values.get(0);
    }

    public void putSingle(Key key, Value value) {
        List<Value> values = findKey(key);
        values.clear();
        values.add(value);
    }

    public void clear() {
        map.clear();
    }

    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    public Set<Entry<Key, List<Value>>> entrySet() {
        return map.entrySet();
    }

    public boolean equals(Object o) {
        return map.equals(o);
    }

    public List<Value> get(Object key) {
        return map.get(key);
    }

    public int hashCode() {
        return map.hashCode();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public Set<Key> keySet() {
        return map.keySet();
    }

    public List<Value> put(Key key, List<Value> value) {
        return map.put(key, value);
    }

    public void putAll(Map<? extends Key, ? extends List<Value>> t) {
        map.putAll(t);
    }

    public List<Value> remove(Object key) {
        return map.remove(key);
    }

    public int size() {
        return map.size();
    }

    public Collection<List<Value>> values() {
        return map.values();
    }

    public void addAll(Key key, Value... newValues) {
        for (Value value : newValues) {
            add(key, value);
        }
    }

    public void addAll(Key key, List<Value> valueList) {
        for (Value value : valueList) {
            add(key, value);
        }
    }

    public void addFirst(Key key, Value value) {
        List<Value> values = get(key);
        if (values == null) {
            add(key, value);
            return;
        } else {
            values.add(0, value);
        }
    }

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

    @SuppressFBWarnings("CN_IDIOM_NO_SUPER_CALL")
    public MultiValuedTreeMap<Key, Value> clone() {
        MultiValuedTreeMap<Key, Value> clone = new MultiValuedTreeMap<>();
        for (Key key : keySet()) {
            List<Value> value = get(key);
            List<Value> newValue = new ArrayList<>();
            newValue.addAll(value);
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
