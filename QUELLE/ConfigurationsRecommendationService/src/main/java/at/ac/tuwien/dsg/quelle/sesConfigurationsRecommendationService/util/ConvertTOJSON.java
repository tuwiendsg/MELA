/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.dsg.quelle.sesConfigurationsRecommendationService.util;

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.requirements.Condition;
import at.ac.tuwien.dsg.mela.common.requirements.Requirement;
import at.ac.tuwien.dsg.mela.common.requirements.Requirements;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.requirements.MultiLevelRequirements;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.requirements.Strategy;
import java.util.ArrayList;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
public class ConvertTOJSON {

    public static String convertTOJSON(List<Metric> metrics) {
        JSONArray children = new JSONArray();

        for (Metric m : metrics) {
            JSONObject metricJSON = new JSONObject();
            metricJSON.put("name", m.getName());
            metricJSON.put("unit", m.getMeasurementUnit());
            metricJSON.put("type", "" +  m.getType());
            children.add(metricJSON);
        }

        return children.toJSONString();
    }

    public static String convertTOJSON(MultiLevelRequirements levelRequirements) {
        if(levelRequirements == null){
            return "{nothing}";
        }
        JSONObject root = new JSONObject();
        //diff is that in some cases (e.g. condition), name is displayName
        root.put("name", levelRequirements.getName());
        root.put("realName", levelRequirements.getName());
        root.put("type", "" + levelRequirements.getLevel());

        //traverse recursive XML tree
        List<JSONObject> jsonObjects = new ArrayList<>();
        List<MultiLevelRequirements> reqsList = new ArrayList<>();

        reqsList.add(levelRequirements);
        jsonObjects.add(root);

        while (!reqsList.isEmpty()) {
            JSONObject obj = jsonObjects.remove(0);
            MultiLevelRequirements reqs = reqsList.remove(0);

            JSONArray children = new JSONArray();
            for (MultiLevelRequirements childReqs : reqs.getContainedElements()) {
                JSONObject child = new JSONObject();
                child.put("name", childReqs.getName());
                child.put("realName", childReqs.getName());
                child.put("type", "" + childReqs.getLevel());
                children.add(child);

                reqsList.add(childReqs);
                jsonObjects.add(child);
            }

            //add strategies
            for (Strategy strategy : reqs.getOptimizationStrategies()) {
                JSONObject strategyJSON = new JSONObject();
                strategyJSON.put("name", "" + strategy.getStrategyCategory().toString());
                strategyJSON.put("realName", "" + strategy.getStrategyCategory().toString());
                strategyJSON.put("type", "Strategy");
                children.add(strategyJSON);
            }

//            JSONArray unitReqs = new JSONArray();
            for (Requirements requirements : reqs.getUnitRequirements()) {
                JSONObject child = new JSONObject();
                child.put("name", requirements.getName());
                child.put("realName", requirements.getName());
                child.put("type", "RequirementsBlock");
                children.add(child);

                JSONArray requirementsJSON = new JSONArray();

                for (Requirement requirement : requirements.getRequirements()) {
                    JSONObject requirementJSON = new JSONObject();
                    requirementJSON.put("name", requirement.getName());
                    requirementJSON.put("realName", requirement.getName());
                    requirementJSON.put("type", "Requirement");
                    //put individual conditions
                    JSONArray conditions = new JSONArray();

                    for (Condition condition : requirement.getConditions()) {
                        JSONObject conditionJSON = new JSONObject();
                        conditionJSON.put("name", "MUST BE " + condition.toString());
                        conditionJSON.put("type", "Condition");
                        conditionJSON.put("realName", "" + condition.getType());
                        conditions.add(conditionJSON);
                    }

                    requirementJSON.put("children", conditions);
                    requirementsJSON.add(requirementJSON);
                }

                child.put("children", requirementsJSON);

            }

            obj.put("children", children);

        }

        return root.toJSONString();
    }
}
