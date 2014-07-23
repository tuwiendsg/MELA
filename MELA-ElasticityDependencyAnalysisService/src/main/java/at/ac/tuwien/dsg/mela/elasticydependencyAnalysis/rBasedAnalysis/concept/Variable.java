/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.dsg.mela.elasticydependencyAnalysis.rBasedAnalysis.concept;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 *
 * Represents a Variable in corelation detection
 */
public class Variable {

    private Map<Object, Object> metaData;
    private List<Double> values;
    private String id;

    public Variable(String id) {
        this.id = id;
    }

    {
        metaData = new HashMap<Object, Object>();
        values = new ArrayList<Double>();
    }

    public void setMetaData(Object key, Object value) {
        metaData.put(key, value);
    }

    public Object getMetaData(Object key) {
        return metaData.get(key);
    }

    public void removeMetaData(Object key) {
        if (metaData.containsKey(key)) {
            metaData.remove(key);
        }
    }

    public Map<Object, Object> getMetaData() {
        return metaData;
    }

    public void setMetaData(Map<Object, Object> metaData) {
        this.metaData = metaData;
    }

    public List<Double> getValues() {
        return values;
    }

    public void setValues(List<Double> values) {
        this.values = values;
    }

    public void addValues(List<Double> values) {
        this.values.addAll(values);
    }

    public void addValue(Double value) {
        this.values.add(value);
    }

    public void removeValue(Double value) {
        this.values.remove(value);
    }

    public void removeValues(List<Double> values) {
        this.values.removeAll(values);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * Modifies variable data in place
     *
     * Will shift data by ROTATION, so if right, last "shiftAmount" positions
     * will be put as first
     *
     * @param shiftAmount number of positions to shift data. Positive means
     * right, negative means left
     * @return the modified variable
     */
    public Variable shiftData(int shiftAmount) {
        if (shiftAmount > 0) {

            for (int i = 0; i < values.size(); i++) {
                int target = i + shiftAmount;
                if (target > values.size() - 1) {
                    target -= values.size() - 1;
                }
                values.set(target, values.get(i));
            }

        } else {
            for (int i = 0; i < values.size(); i++) {
                int target = i + shiftAmount;
                if (target < 0) {
                    target += values.size() - 1;
                }
                values.set(target, values.get(i));
            }

        }

        return this;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Variable other = (Variable) obj;
        if ((this.id == null) ? (other.id != null) : !this.id.equals(other.id)) {
            return false;
        }
        return true;
    }

}
