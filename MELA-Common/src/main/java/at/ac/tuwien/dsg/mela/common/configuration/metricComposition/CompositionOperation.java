/**
 * Copyright 2013 Technische Universitat Wien (TUW), Distributed Systems Group
 * E184
 *
 * This work was partially supported by the European Commission in terms of the
 * CELAR FP7 project (FP7-ICT-2011-8 \#317790)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package at.ac.tuwien.dsg.mela.common.configuration.metricComposition;

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement.MonitoredElementLevel;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElementMonitoringSnapshot;
import java.util.ArrayList;
import java.util.List;
import javax.security.auth.login.Configuration;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at *
 *
 * Supports multiple operations. Such as "DIV (SUM COST) (KEEP CONNECTIONS) or
 * SUM( KEEP COST)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Operation")
public class CompositionOperation {

    //TODO: Add source element ID (example, from a topology, I want ResponseTime from only 2 Units, not all)
    @XmlAttribute(name = "type", required = true)
    private CompositionOperationType operationType;
    @XmlAttribute(name = "value")
    private String value;
    @XmlElement(name = "ReferenceMetric")
    private Metric referenceMetric;
    @XmlAttribute(name = "MetricSourceMonitoredElementLevel", required = true)
    private MonitoredElement.MonitoredElementLevel metricSourceMonitoredElementLevel;
    @XmlElement(name = "SourceMonitoredElementID", required = false)
    private ArrayList<String> metricSourceMonitoredElementIDs;
    @XmlElement(name = "Operation", required = true)
    private ArrayList<CompositionOperation> subOperations;

    {
        subOperations = new ArrayList<CompositionOperation>();
        metricSourceMonitoredElementIDs = new ArrayList<String>();

    }

    public ArrayList<String> getMetricSourceMonitoredElementIDs() {
        return metricSourceMonitoredElementIDs;
    }

    public void setMetricSourceMonitoredElementIDs(ArrayList<String> metricSourceMonitoredElementIDs) {
        this.metricSourceMonitoredElementIDs = metricSourceMonitoredElementIDs;
    }

    public void addMetricSourceMonitoredElementID(String id) {
        metricSourceMonitoredElementIDs.add(id);
    }

    public void removeMetricSourceMonitoredElementID(String id) {
        metricSourceMonitoredElementIDs.remove(id);
    }

    public MonitoredElementLevel getMetricSourceMonitoredElementLevel() {
        return metricSourceMonitoredElementLevel;
    }

    public void setMetricSourceMonitoredElementLevel(MonitoredElementLevel metricSourceMonitoredElementLevel) {
        this.metricSourceMonitoredElementLevel = metricSourceMonitoredElementLevel;
    }

    public CompositionOperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(CompositionOperationType operation) {
        this.operationType = operation;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Metric getTargetMetric() {
        return referenceMetric;
    }

    public void setTargetMetric(Metric targetMetric) {
        this.referenceMetric = targetMetric;
    }

    public ArrayList<CompositionOperation> getSubOperations() {
        return subOperations;
    }

    public void setSubOperations(ArrayList<CompositionOperation> subOperations) {
        this.subOperations = subOperations;
    }

    public void addCompositionOperation(CompositionOperation compositionOperation) {
        subOperations.add(compositionOperation);
    }

    public void removeCompositionOperation(CompositionOperation compositionOperation) {
        subOperations.remove(compositionOperation);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CompositionOperation that = (CompositionOperation) o;

        if (operationType != that.operationType) {
            return false;
        }
//        if (referenceMetric != null ? !referenceMetric.equals(that.referenceMetric) : that.referenceMetric != null) {
//            return false;
//        }
        if (value != null ? !value.equals(that.value) : that.value != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = operationType != null ? operationType.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
//        result = 31 * result + (referenceMetric != null ? referenceMetric.hashCode() : 0);
        return result;
    }

    /**
     * @param elementMonitoringSnapshot the snapshot of the service element in
     * which this operation is applied. The idea is to have a CompositionRule
     * which has target ServiceIDs and Level, and for each extracted ServiceID,
     * applies the supplied set of operations
     * @return a single value as result
     */
    public MetricValue apply(MonitoredElementMonitoringSnapshot elementMonitoringSnapshot) {

        MetricValue result = new MetricValue(-1);

        // operations done on result
        // for example SUM will place the sum on result[0]
        // method returns result[0]
        List<MetricValue> valuesToBeProcessed = new ArrayList<MetricValue>();

        //1'st step, extract the target metric if there is such metric
        MonitoredElement MonitoredElement = elementMonitoringSnapshot.getMonitoredElement();

        //target metric might be null if a metric is created using the SET_VALUE operation type
        if (referenceMetric != null) {
            //if the target metric is at this element, extract it, otherwise search it, else
            //search in the element children (composition rules search metrics for the target element level and the direct sub-level)
            if (this.getMetricSourceMonitoredElementLevel().equals(MonitoredElement.getLevel())) {
                if (elementMonitoringSnapshot.containsMetric(referenceMetric)) {
                    valuesToBeProcessed.add(elementMonitoringSnapshot.getValueForMetric(referenceMetric).clone());
                } else {
                    Logger.getRootLogger().log(Level.WARN, "Metric " + referenceMetric + " not found in " + MonitoredElement.getId());
//                    return null;
                }
            } else {
                List<MonitoredElementMonitoringSnapshot> childSnapshotForSpecificLevel = new ArrayList<MonitoredElementMonitoringSnapshot>();
                for (MonitoredElementMonitoringSnapshot childSnapshot : elementMonitoringSnapshot) {
                    if (metricSourceMonitoredElementIDs.isEmpty() || metricSourceMonitoredElementIDs.contains(childSnapshot.getMonitoredElement().getId())) {
                        if (childSnapshot.getMonitoredElement().getLevel().equals(metricSourceMonitoredElementLevel)) {
                            childSnapshotForSpecificLevel.add(childSnapshot);
                        }
                    }
                }
                for (MonitoredElementMonitoringSnapshot childSnapshot : childSnapshotForSpecificLevel) {
                    //if source IDs have been supplied check if the snapshot belongs to a specified ID
                    if (childSnapshot.containsMetric(referenceMetric)) {
                        valuesToBeProcessed.add(childSnapshot.getValueForMetric(referenceMetric).clone());
                    } else {
//                        Logger.getRootLogger().log(Level.WARN, "Metric " + referenceMetric + " not found in " + childSnapshot.getMonitoredElement().getId());
//                        return null;
                    }
                }
            }
        }

        //2'nd step
        //if there are sub/operations to be applied, apply them, and then apply THIS.
        //the idea is to be able to say "for metric Children from Service level, divide with (SUM Cost (Service topology level))

        for (CompositionOperation subOperation : this.subOperations) {
            MetricValue subOperationValue = subOperation.apply(elementMonitoringSnapshot);
            if (subOperationValue != null) {
                valuesToBeProcessed.add(subOperationValue);
            }
        }

        //if the operation includes a simple value, try to convert to double 
        //enables for example : for VM, set VMCount = 1;
        Double operator = 0.0d;
        if (value != null && value.length() > 0) {
            operator = Double.parseDouble(value);
        }

        //apply operation on sequence of values
        switch (operationType) {
            case ADD:
                if (!valuesToBeProcessed.isEmpty()) {
                    for (MetricValue metricValue : valuesToBeProcessed) {
                        if (metricValue.getValueType() == MetricValue.ValueType.NUMERIC) {
                            metricValue.setValue(((Number) metricValue.getValue())
                                    .doubleValue() + operator);
                        }
                    }
                    result = valuesToBeProcessed.get(0);
                }
                break;
            case AVG: {
                if (!valuesToBeProcessed.isEmpty()) {
                    Double avg = 0.0d;
                    for (MetricValue metricValue : valuesToBeProcessed) {
                        if (metricValue.getValueType() == MetricValue.ValueType.NUMERIC) {
                            avg += (((Number) metricValue.getValue()).doubleValue());
                        }
                    }
                    MetricValue metricValue = new MetricValue();
                    metricValue.setValue(avg / valuesToBeProcessed.size());
                    result = metricValue;
                }
            }
            break;
            case CONCAT: {
                if (!valuesToBeProcessed.isEmpty()) {
                    String concat = "";
                    for (MetricValue metricValue : valuesToBeProcessed) {
                        concat += metricValue.getValue().toString() + ",";
                    }
                    MetricValue metricValue = new MetricValue();
                    metricValue.setValue(concat);
                    result = metricValue;
                }
            }
            break;
            case DIV: {
                if (!valuesToBeProcessed.isEmpty()) {
                    MetricValue metricValue = new MetricValue();
                    Double firstOperand = ((Number) valuesToBeProcessed.get(0).getValue()).doubleValue();
                    Double secondOperand;

                    if (valuesToBeProcessed.size() > 1) {
                        secondOperand = ((Number) valuesToBeProcessed.get(1).getValue()).doubleValue();
                    } else {
                        secondOperand = operator;
                    }

                    if (secondOperand != 0) {
                        metricValue.setValue(firstOperand / secondOperand);

                    } else {
                        metricValue.setValue(firstOperand);
                    }

                    result = metricValue;
                }
            }
            break;
            case KEEP:
                //in the case in which the metric was not found at this snapshot
                if (!valuesToBeProcessed.isEmpty()) {
                    result = valuesToBeProcessed.get(0);
                }
                break;
            case MAX: {
                if (!valuesToBeProcessed.isEmpty()) {
                    Double max = 0.0d;
                    for (MetricValue metricValue : valuesToBeProcessed) {
                        if (metricValue.getValueType() == MetricValue.ValueType.NUMERIC) {
                            if (max < (((Number) metricValue.getValue())
                                    .doubleValue())) {
                                max = (((Number) metricValue.getValue())
                                        .doubleValue());
                            }
                        }
                    }
                    MetricValue metricValue = new MetricValue();
                    metricValue.setValue(max);
                    result = metricValue;
                }
            }
            break;
            case MIN: {
                if (!valuesToBeProcessed.isEmpty()) {
                    Double min = 0.0d;
                    for (MetricValue metricValue : valuesToBeProcessed) {
                        if (metricValue.getValueType() == MetricValue.ValueType.NUMERIC) {
                            if (min > (((Number) metricValue.getValue())
                                    .doubleValue())) {
                                min = (((Number) metricValue.getValue())
                                        .doubleValue());
                            }
                        }
                    }
                    MetricValue metricValue = new MetricValue();
                    metricValue.setValue(min);
                    result = metricValue;
                }
            }
            break;
            case MUL: {
                if (!valuesToBeProcessed.isEmpty()) {
                    MetricValue metricValue = new MetricValue();
                    Double firstOperand = ((Number) valuesToBeProcessed.get(0).getValue()).doubleValue();
                    Double secondOperand;

                    if (valuesToBeProcessed.size() > 1) {
                        secondOperand = ((Number) valuesToBeProcessed.get(1).getValue()).doubleValue();
                    } else {
                        secondOperand = operator;
                    }

                    metricValue.setValue(firstOperand * secondOperand);

                    result = metricValue;
                }
            }
            break;
            case SUB:
                if (!valuesToBeProcessed.isEmpty()) {
                    for (MetricValue metricValue : valuesToBeProcessed) {
                        if (metricValue.getValueType() == MetricValue.ValueType.NUMERIC) {
                            metricValue.setValue((((Number) metricValue.getValue())
                                    .doubleValue()) - operator);
                        }
                    }
                    result = valuesToBeProcessed.get(0);
                }
                break;
            case SUM: {
                if (!valuesToBeProcessed.isEmpty()) {
                    Double sum = 0.0d;
                    for (MetricValue metricValue : valuesToBeProcessed) {
                        if (metricValue.getValueType() == MetricValue.ValueType.NUMERIC) {
                            sum += (((Number) metricValue.getValue()).doubleValue());
                        }
                    }
                    MetricValue metricValue = new MetricValue();
                    metricValue.setValue(sum);
                    result = metricValue;
                }
            }
            break;
            case UNION:
                break;
            case KEEP_LAST: {
                if (!valuesToBeProcessed.isEmpty()) {
                    MetricValue metricValue = valuesToBeProcessed.get(valuesToBeProcessed.size() - 1);
                    result = metricValue;
                }
            }
            break;
            case KEEP_FIRST: {
                if (!valuesToBeProcessed.isEmpty()) {
                    MetricValue metricValue = valuesToBeProcessed.get(0);
                    result = metricValue;
                }
            }
            break;
            case SET_VALUE: {
                MetricValue metricValue = new MetricValue();
                metricValue.setValue(operator);
                result = metricValue;
            }
            break;
            default:
                Logger.getRootLogger().log(Level.WARN, "Operation type " + operationType + " not recognized");
                result = new MetricValue();
                break;

        }

        return result;
    }

    //used in historical metric
    public MetricValue apply(List<MetricValue> values) {

        MetricValue result = new MetricValue();

        // operations done on result
        // for example SUM will place the sum on result[0]
        // method returns result[0]
        List<MetricValue> valuesToBeProcessed = new ArrayList<MetricValue>();
        valuesToBeProcessed.addAll(values);

        for (CompositionOperation subOperation : this.subOperations) {
            valuesToBeProcessed.add(subOperation.apply(values));
        }

        //if the operation includes a simple value, try to convert to double 
        //enables for example : for VM, set VMCount = 1;
        Double operator = 0.0d;
        if (value != null && value.length() > 0) {
            operator = Double.parseDouble(value);
        }

        //apply operation on sequence of values
        switch (operationType) {
            case ADD:
                for (MetricValue metricValue : valuesToBeProcessed) {
                    if (metricValue.getValueType() == MetricValue.ValueType.NUMERIC) {
                        metricValue.setValue(((Number) metricValue.getValue())
                                .doubleValue() + operator);
                    }
                }
                break;
            case AVG: {
                Double avg = 0.0d;
                for (MetricValue metricValue : valuesToBeProcessed) {
                    if (metricValue.getValueType() == MetricValue.ValueType.NUMERIC) {
                        avg += (((Number) metricValue.getValue()).doubleValue());
                    }
                }
                MetricValue metricValue = new MetricValue();
                metricValue.setValue(avg / valuesToBeProcessed.size());
                result = metricValue;
            }
            break;
            case CONCAT: {
                String concat = "";
                for (MetricValue metricValue : valuesToBeProcessed) {
                    concat += metricValue.getValue().toString() + ",";
                }
                MetricValue metricValue = new MetricValue();
                metricValue.setValue(concat);
                result = metricValue;
            }
            break;
            case DIV:
                for (MetricValue metricValue : valuesToBeProcessed) {
                    if (metricValue.getValueType() == MetricValue.ValueType.NUMERIC) {
                        if (operator != 0) {
                            metricValue.setValue((((Number) metricValue
                                    .getValue()).doubleValue()) / operator);
                        } else {
                            metricValue.setValue((((Number) metricValue
                                    .getValue()).doubleValue()));
                        }
                    }
                }
                break;
            case KEEP:
                break;
            case MAX: {
                Double max = 0.0d;
                for (MetricValue metricValue : valuesToBeProcessed) {
                    if (metricValue.getValueType() == MetricValue.ValueType.NUMERIC) {
                        if (max < (((Number) metricValue.getValue())
                                .doubleValue())) {
                            max = (((Number) metricValue.getValue())
                                    .doubleValue());
                        }
                    }
                }
                MetricValue metricValue = new MetricValue();
                metricValue.setValue(max);
                result = metricValue;
            }
            break;
            case MIN: {
                Double min = 0.0d;
                for (MetricValue metricValue : valuesToBeProcessed) {
                    if (metricValue.getValueType() == MetricValue.ValueType.NUMERIC) {
                        if (min > (((Number) metricValue.getValue())
                                .doubleValue())) {
                            min = (((Number) metricValue.getValue())
                                    .doubleValue());
                        }
                    }
                }
                MetricValue metricValue = new MetricValue();
                metricValue.setValue(min);
                result = metricValue;
            }
            break;
            case MUL:
                for (MetricValue metricValue : valuesToBeProcessed) {
                    if (operator != 0) {
                        metricValue.setValue((((Number) metricValue.getValue())
                                .doubleValue()) * operator);
                    } else {
                        metricValue.setValue((((Number) metricValue.getValue())
                                .doubleValue()));
                    }
                }
                break;
            case SUB:
                for (MetricValue metricValue : valuesToBeProcessed) {
                    if (metricValue.getValueType() == MetricValue.ValueType.NUMERIC) {
                        metricValue.setValue((((Number) metricValue.getValue())
                                .doubleValue()) - operator);
                    }
                }
                break;
            case SUM: {
                Double sum = 0.0d;
                for (MetricValue metricValue : valuesToBeProcessed) {
                    if (metricValue.getValueType() == MetricValue.ValueType.NUMERIC) {
                        sum += (((Number) metricValue.getValue()).doubleValue());
                    }
                }
                MetricValue metricValue = new MetricValue();
                metricValue.setValue(sum);
                result = metricValue;
            }
            break;
            case UNION:
                break;
            case KEEP_LAST: {
                MetricValue metricValue = valuesToBeProcessed.get(valuesToBeProcessed.size() - 1);
                result = metricValue;
            }
            break;
            case KEEP_FIRST: {
                MetricValue metricValue = valuesToBeProcessed.get(0);
                result = metricValue;
            }
            break;
            case SET_VALUE: {
                MetricValue metricValue = new MetricValue();
                metricValue.setValue(operator);
                result = metricValue;
            }
            break;
            default:
                Logger.getRootLogger().log(Level.WARN, "Operation type " + operationType + " not recognized");
                result = new MetricValue();
                break;

        }

        return result;
    }
}
