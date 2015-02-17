function include(filename)
{
    var head = document.getElementsByTagName('head')[0];

    var script = document.createElement('script');
    script.src = filename;
    script.type = 'text/javascript';

    head.appendChild(script)
}
//
//include("http://ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js");
//include("http://ajax.googleapis.com/ajax/libs/jquery/1.9.1/jquery.min.js");
//include("http://code.jquery.com/ui/1.11.1/themes/smoothness/jquery-ui.css");
//include("http://code.jquery.com/jquery-1.10.2.js");
//include("http://code.jquery.com/ui/1.11.1/jquery-ui.js");
//include("http://d3js.org/d3.v3.js");


function contains(children, child) {
    for (var index = 0; index < children.length; index++) {
        if (children[index].name == child.name) {
            return 0;
        }
    }
    return -1;
}

function updateTextOnNode(node, new_root, change) {

    if (node.actionName || new_root.actionName) {
        //node.name = new_root.name + ": " + new_root.actionName;
        node.attention = new_root.attention;
        node.actionName = new_root.actionName;
    }

    node.name = new_root.name;
    if (!node.children) {
        node.children = [];
    }


    //check if some new nodes have appeared
    if (node.children.length < new_root.children.length) {
        //for all new children
        for (var index = 0; index < new_root.children.length; index++) {
            //if the children are not metrics
            if (new_root.children[index].type.match(/SERVIC.*|V.*/g)) {
                //if new root child DOES NOT ALLREADY EXIST
                if (contains(node.children, new_root.children[index]) == -1) {
                    node.children.push(new_root.children[index]);
                    update(node);
                    change = 0;
                }
            }
        }
    }

    //check if nodes need to be removed
    if (node.children.length > new_root.children.length) {
        for (var index = 0; index < node.children.length; index++) {
            if (node.children[index].type.match(/SERVIC.*|V.*/g)) {
                if (contains(new_root.children, node.children[index]) == -1) {
                    node.children.splice(index, 1);
                    change = 0;
                }
            }
        }
    }

    //remove all metrics, requirements and conditions so that they can be updated
    for (var index = 0; index < node.children.length; index++) {
        var oldEntry = node.children[index];
        if (!oldEntry.type.match(/SERVIC.*|V.*/g)) {
            node.children.splice(index, 1);
            //start over
            index = -1;
        }
    }


    //add all new metrics so that they can be updated
    for (var index = 0; index < new_root.children.length; index++) {
        var newEntry = new_root.children[index];
        if (!newEntry.type.match(/SERVIC.*|V.*/g)) {
            node.children.push(newEntry);
        }
    }

    //update all children not metrics
    for (var index = 0; index < node.children.length; index++) {
        var oldEntry = node.children[index];
        if (oldEntry.type.match(/SERVIC.*|V.*/g)) {
            //find the element int the new children that matches this
            for (var indexNew = 0; indexNew < new_root.children.length; indexNew++) {
                var newEntry = new_root.children[indexNew];
                if (newEntry.type.match(/SERVIC.*|V.*/g)) {
                    if (oldEntry.name == newEntry.name) {
                        updateTextOnNode(oldEntry, newEntry);
                    }
                }
            }
        }
    }

}


var selectedMetric;
var simpleComponentIcon = "m 5,10 c 1.7265,0.251 5.7035,0.0355 4.8055,2.6145 -0.9305,2.0335 -3.066,3.827 0.214,4.8855 1.9925,0.6435 10.717,1.143 9.7905,-2.5835 -1.1255,-1.2255 -2.5535,-2.4125 -1.2315,-4.0245 2.8935,-0.552 5.8135,-0.9665 8.747,-1.2365 2.779,-0.2555 5.01138,-0.3785 7.80388,-0.3535 0,0 0.0342,-28.8233 0,-28.782 l -42.17988,0 c -0.7375,3.8525 -0.9175,8.9665 1.1535,10.61 3.0355,1.834 7.6995,-3.225 9.5015,0.7885 1.384,3.0825 -0.1075,8.324 -4.242,6.515 -4.9185,-2.1525 -7.189,0.88 -6.7055,6.19 0.1545,1.6955 0.472,3.214 0.701,4.702 3.891,-0.081 7.791,0.114 11.642,0.6745 z";

var complexComponentIcon = "m -10,0 c 3.73224,-0.7459 8.66496,-0.9953 8.05062,0.63028 l -0.81288,2.33051 c 0.0832,1.10156 6.73944,1.38304 6.33894,-0.31885 0,0 -1.18264,-2.45972 -0.99342,-2.50527 -0.0569,-0.88313 8.32576,-0.86545 8.32576,-0.86545 0.78063,1.41974 -0.96421,4.29648 -0.50291,5.19887 1.09973,2.15125 4.95457,0.51254 5.20531,0.70421 0.63103,0.48237 0.96734,3.49919 -0.33288,3.38175 -2.20415,-0.19909 -6.72157,-1.93952 -4.27491,6.24781 l 21.61861,0.3644 -0.33114,-16.07925 c -2.69909,-0.38953 -8.50495,0.33626 -8.33363,1.04589 0.94358,3.90859 -2.59923,4.22934 -5.08229,3.00142 -0.66963,-0.36714 0.47037,-2.20109 0.10252,-2.99309 -0.78827,-1.28418 -3.69368,-0.8065 -8.16329,-0.96312 0,0 -0.70958,-4.82833 -0.42575,-5.05608 2.19333,-0.41775 5.58828,0.77701 5.69185,-2.38338 0.29332,-2.55231 -2.1638,-2.06746 -4.59029,-1.46068 -1.2562,0.31413 -1.57753,-3.06109 -1.19597,-5.67595 l -20.34134,0.0911 0.0473,30.38204 42.43301,-0.1822 0.18922,-30.29094 -22.42279,0";

var warningTriangle = "m -25,10 11.66727,-22.45064 11.66726,22.45064 z";
var sphere = "m 0,0 a 10.253048,9.8994951 0 1 1 -4e-5,-0.003";
var octogon = "m 0,-5 6.63341,-7.14443 11.95156,0 6.46184,7.14443 0,8.97271 -6.46184,6.58185 -11.95156,0 -6.63341,-6.58185 z";
var auxRect = "m -32,5 0,-13.25825 30.14043,0 0,13.25825 z"

var diagonal = d3.svg.diagonal()
        .projection(function (d) {
            return [d.y, d.x];
        });


var basisLineFunction = d3.svg.line()
        .x(function (d) {
            return d.x;
        })
        .y(function (d) {
            return d.y;
        })
        .interpolate("basis");


var treeVisualisationMargin = {top: 10, right: 0, bottom: 20, left: 10};
var tree;
var treeVisualization;
var treeRootNode;
var spaceType;
var serviceID;
function setupTreeView(service, targetDIV_ID, w, h, type) {
    serviceID = this.service;
    if(!spaceType){
        spaceType = "instant";
    }else{
        spaceType = type;
    }

    width = w;
    height = h;
    i = 0;
    duration = 500;
    depth = width / 4.5;

    tree = d3.layout.tree()
            .size([height, width]);

    treeVisualization = d3.select("#" + targetDIV_ID).append("svg")
            .attr("width", width + treeVisualisationMargin.right + treeVisualisationMargin.left)
            .attr("height", height + treeVisualisationMargin.top + treeVisualisationMargin.bottom)
            .append("g")
            .attr("transform", "translate(" + treeVisualisationMargin.left + "," + treeVisualisationMargin.top + ")");

}



var colors = ["#E00000", "#CCFFFF"];
var legend = ["legend", "component", "metric", "requirement"];
var labels = ["Legend", "Monitored Element", "Metric", "Requirement"];


function expandTree(rootNode) {
    var expanded = [];

    expanded.push(rootNode);
    if (rootNode.children) {
        for (var i = 0; i < rootNode.children.length; i++) {
            var expandedChildren = expandTree(rootNode.children[i]);
            for (var j = 0; j < expandedChildren.length; j++) {
                expanded.push(expandedChildren[j]);
            }
        }
    }

    return expanded;
}


function clean(d, nodeType) {

    var children = d.children;
    if (children) {
        for (var i = children.length - 1; i--; ) {
            if (children[i].name == nodeType)
                children[i].remove()
        }

        for (var i = children.length - 1; i--; ) {
            clean(children[i], nodeType);
        }
    }
}


function update(source) {

    // Compute the new tree layout.
    var nodes = tree.nodes(treeRootNode).reverse();

    //                // Normalize for fixed-depth.
    nodes.forEach(function (d) {
        if (d.type == "requirement") {
            d.y = 0.8 * (d.depth * depth);
        } else if (!d.type.match(/SERVIC.*|V.*/g) || (d.type == "requirement")) {
            if (d.parent.children) {
                var hasComplexChildren;
                var children = d.parent.children;
                if (children) {
                    for (var i = children.length - 1; i--; ) {
                        if (children[i].type.match(/SERVIC.*|V.*/g)) {
                            hasComplexChildren = true;
                            break;
                        }
                    }
                    d.y = 0.85 * d.depth * depth;
                }
            }

        } else {
            d.y = d.depth * depth;
        }
    });

    // Update the nodes…
    var node = treeVisualization.selectAll("g.node")
            .data(nodes, function (d) {
                return d.id || (d.id = ++i);
            });

    node.selectAll("text")
            .text(function (d) {
                if (d.attention) {
                    return d.name + ": " + d.actionName;
                } else if (d.type == "VM") {
                    return d.name;
                } else {
                    return d.name;
                }
            });

    // Enter any new nodes at the parent's previous position.
    var nodeEnter = node.enter().append("g")
            .attr("class", "node")
            .attr("transform", function (d) {
                return "translate(" + source.y + "," + source.x + ")";
            })
            .attr("display", function (d) {
                if (d.name == "SubComponents") {
                    return "none";
                } else {
                    return "yes"
                }
                ;
            })
            .on("click", function (d) {
                if (!d.type.match(/SERVIC.*|V.*/g)) {
                    setSelectedMetricOneLevel(d);
                }
                else {
                    openElasticitySpaceOrPathway(d);
                }
            })
            .on("dblclick", setSelectedMetric);

    nodeEnter.append("text")
            .attr("text-anchor", function (d) {
                var position = 0;
                switch (d.type) {
                    case "SERVICE":
                        position = "start";
                        break;
                    case "metric":
                        position = "start";
                        break;
                    default:
                        position = "end";
                        break;

                }
                return position;

            })

            .attr("dy", function (d) {
                var position = 0;
                switch (d.type) {
                    case "SERVICE":
                        position = -25;
                        break;
                    case "VM":
                        position = -15;
                        break;
                    default:
                        position = -5;
                        break;

                }
                return position;

            })
            .attr("dx", function (d) {
                var position = 0;
                switch (d.type) {
                    case "SERVICE":
                        position = -10;
                        break;
                    case "VM":
                        position = +25;
                        break;
                    case "metric":
                        position = 5;
                        break;
                    default:
                        position = -15;
                        break;
                }
                return position;

            })


            .style("font-size", function (d) {
                return (d.type == "metric") ? 14 : 18;
            })
            .attr("font-style", function (d) {
                return d.children ? "normal" : "italic";
            })
            .style("fill-opacity", 1)
            .text(function (d) {
                if (d.attention) {
                    return d.name + ": " + d.actionName;
                } else if (d.type == "VM") {
                    return d.name;
                } else {
                    return d.name;
                }
            });



    nodeEnter.append("path")
            .attr("d", function (d) {
                if (d.type == "SERVICE" || d.type == "SERVICE_TOPOLOGY" || d.type == "SERVICE_UNIT") {
                    return simpleComponentIcon;
                }

                else if (d.type == "metric") {
                    return auxRect;
                }
                else if (d.type == "auxiliaryMetric") {
                    return auxRect;
                }
                else if (d.type == "requirement") {
                    if (d.attention) {
                        return warningTriangle;
                    } else {
                        return sphere;
                    }
                    ;
                }
                else if (d.name == "SubComponents") {
                    return complexComponentIcon;
                }
            }
            )
            .attr("stroke", "black")
            .attr("stroke-width", 1)
            .style("fill", function (d) {
                if (d.attention) {
                    return "#FF6666";
                } else {
                    return "#CCFFFF";
                }
            });





    nodeEnter.append("svn:image")
            .attr("xlink:href", function (d) {
                if (d.type == "VM") {
                    return "./vm.png";
                } else {
                    return null;
                }
            })
            .attr("width", 30)
            .attr("height", 30)
            .attr("dx", -15)
            .attr("y", -15);

    // Transition nodes to their new position.

    var nodeUpdate = node.transition()

            .duration(0)
            .attr("transform", function (d) {
                if (d.type != "requirement") {
                    return "translate(" + d.y + "," + d.x + ")";
                } else {
                    return "translate(" + d.y + "," + d.x + ")";
                }
            })

    //console.log(node.name)




    nodeUpdate.select("path")
            .attr("r", function (d) {
                return 4.5;
            })
            .style("stroke", function (d) {
                if (d.attention) {
                    return "#909090";
                } else {
                    return "#909090"
                }
                ;
            })
            .style("fill", function (d) {
                if (d.type == "metric") {
                    return "gray";
                } else {
                    if (d.attention) {
                        return "#D13F31";
                    } else {
                        if (d.type == "requirement") {
                            if (d.fulfilled) {
                                return "#1F7872"
                            } else {
                                return "#D13F31";
                            }

                        } else {
                            return "#72B095";
                        }
                    }
                }
            }
            );


    // Transition exiting nodes to the parent's new position.
    var nodeExit = node.exit().transition()
            .duration(function (d) {
                if (d.type.match(/SERVIC.*|V.*/g)) {
                    return duration;
                } else {
                    return 0;
                }
            })
            .attr("transform", function (d) {
                return "translate(" + source.y + "," + source.x + ")";
            })
            .remove();

    nodeExit.select("circle")
            .attr("r", function (d) {
                return d.value ? 0 : 8;
            });

    nodeExit.select("text")
            .attr("text-anchor", function (d) {
                return d.value || d.ip || d.children ? "end" : "start";
            })
            .attr("dy", -5)
            .style("font-size", function (d) {
                return (d.type == "metric") ? 14 : 18;
            })
            .attr("font-style", function (d) {
                return d.children ? "normal" : "italic";
            })
            .style("fill-opacity", 1e-6);
//
//    nodeEnter.append("text")
//            .attr("dx", function (d) {
//                return d.value ? 10 : 5;
//            })
//            .attr("dy", function (d) {
//                return d.value ? 0 : 10;
//            })
//            .style("font-size", function (d) {
//                return (d.type == "metric") ? 14 : 18;
//            })
//            .attr("text-anchor", function (d) {
//                return d.ip ? "end" : "start";
//            })
//            .attr("font-style", function (d) {
//                return d.children ? "normal" : "italic";
//            })
//            .text(function (d) {
//                if (d.attention) {
//                    return d.name + ": " + d.actionName;
//                } else if (d.type == "VM") {
//                    return d.name;
//                } else {
//                    return d.name;
//                }
//            });

    // Update the links…
    var link = treeVisualization.selectAll("path.link")
            .data([], function (d) {
                return d.target.id;
            });
    link.exit().remove();


    link = treeVisualization.selectAll("path.link")
            .data(tree.links(nodes), function (d) {
                return d.target.id;
            });

    // Enter any new links at the parent's previous position.
    link.enter().insert("path", "g")
            .attr("class", "link")
            .attr("d", function (d) {
                var o = {x: source.x, y: source.y};
                return diagonal({source: o, target: o});
            })
            .style("stroke-dasharray", function (d) {
                if (d.target.type == "metric") {
                    return "0";
                }
                else if (d.target.type == "requirement") {
                    return "3.3";
                }
                else if (d.target.type == "auxiliaryMetric") {
                    return "3.3";
                }
                else {
                    return "1";
                }
            })
            .style("stroke", function (d) {
                if (d.target.type == "requirement") {
                    if (d.target.attention) {
                        return "#E00000";
                    }
                    else {
                        return "#00E096";
                    }
                } else {
                    return "#ccc";
                }
            })
            .style("stroke-width", function (d) {
                if (d.target.type == "requirement") {
                    return "1";
                }
                else if (d.target.type == "auxiliaryMetric") {
                    return "0.5";
                }
                else {
                    return "1";
                }
            })
            ;

    // Transition links to their new position.
    link.transition()
            .duration(0)
            .attr("d", diagonal);

    // Transition exiting nodes to the parent's new position.
    link.exit().transition()
            .duration(0)
            .attr("d", function (d) {
                var o = {x: source.x, y: source.y};
                return diagonal({source: o, target: o});
            })
            .remove();

    // Stash the old positions for transition.
    nodes.forEach(function (d) {
        d.x0 = d.x;
        d.y0 = d.y;
    });
}

var fullPath = false;
function setSelectedMetric(s) {

    treeVisualization.selectAll("path.metricLink").remove();

    //make it a toggle. If the same metric is clicked twice do not redraw
    if (selectedMetric != s) {
        selectedMetric = s;
        highlightMetricsSources(selectedMetric);
    } else {
        selectedMetric = null;
    }
    fullPath = true;

}


function setSelectedMetricOneLevel(s) {

    treeVisualization.selectAll("path.metricLink").remove();

    //make it a toggle. If the same metric is clicked twice do not redraw
    if (selectedMetric != s) {
        selectedMetric = s;
        highlightMetricsSourcesOneLevel(selectedMetric);
    } else {
        selectedMetric = null;
    }
    fullPath = false;

}


function showContent(d, show) {
    if (!show && d.children) {
        d._children = d.children;
        d.children = null;
        update(d);
    } else if (!d.children) {
        d.children = d._children;
        d._children = null;
        update(d);
    }

}

function drawMetricsLines(source, componentsToLink, link) {

    for (var i = 0; i < componentsToLink.length; i++) {

        var child = componentsToLink[i];

        var childMetricNameIndexOfFirstBracket = child.name.match(/[a-zA-Z]/);
        var childMetricNameIndexOfLastBracket = child.name.match(/ \(/);
        var childMetricName = child.name.slice(childMetricNameIndexOfFirstBracket.index, childMetricNameIndexOfLastBracket.index);


        treeVisualization.append("marker")
                .attr("id", function () {
                    return "marker_" + child.parent.name + "_" + childMetricName;
                })
                .attr("viewBox", "0 -5 10 10")
                .attr("refX", 0)
                .attr("refY", 0)
                .attr("markerWidth", 3)
                .attr("markerHeight", 3)
                .attr("orient", "auto")
                .append("path")
                .attr("d", "M0,-5L10,0L0,5");

        treeVisualization.append("path", "g")
                .attr("class", "metricLink")
                .attr("startAngle", "45")

                .attr("d", function (d) {
                    var start = {x: source.x, y: source.y};
                    var target = {x: child.x, y: child.y};
                    //avoid drawing straight lines
                    //comparing Y because the layout is reversed, is horizontal, so Y plays for X
                    if (source.y == child.y) {
                        start = {y: source.x, x: source.y};
                        target = {y: child.x, x: child.y};
                        lineData = [start, {x: start.x + 20, y: start.y}, {x: target.x + 20, y: target.y}, target];
                        return basisLineFunction(lineData);
                    } else {
                        return diagonal({source: start, target: target});
                    }


                })
                .attr("marker-end", function () {
                    return "url(#marker_" + child.parent.name + "_" + childMetricName + ")";
                });

    }


}

function highlightMetricsSources(selectedMetric) {
    if (!metrics || !selectedMetric) {
        return
    }


    var source = selectedMetric;



    var indexOfFirstBracket = source.name.match(/[a-zA-Z]/);
    var indexOfLastBracket = source.name.match(/ \(/);
    var sourceMetricName = source.name.slice(indexOfFirstBracket.index, indexOfLastBracket.index);

    var sourceMetricParent = source.parent;

    //traverse metrics and find out what metric we have selected 
    var compositionRule;
    for (var i = 0; i < metrics.length; i++) {
        metric = metrics [i];
        if (metric.name == sourceMetricName) {
            //true even if is empty
            if (metric.targetMonitoredElementIDs) {

                //get the targeted element IDS
                targetMonitoredElementIDs = metric.targetMonitoredElementIDs;

                //if the metric level is the same as the clicked metric parent type
                if (metric.targetLevel && metric.targetLevel == sourceMetricParent.type) {

                    //if the targete lement IDs == 0 || if the target metric parent name is in the supplied IDs
                    if (targetMonitoredElementIDs.length == 0 || targetMonitoredElementIDs.indexOf(sourceMetricParent.name) != -1) {
                        compositionRule = metric;
                        break;
                    }
                }
            }
        }
    }

    if (!compositionRule) {
        return;
    }


    var sourceMetricParentChildMetrics = [];
    var ruleChildMetrics = [];

    //1'st go to the targeted element children (children of clicked sourceMetricParent)
    if (sourceMetricParent.children) {
        var children = [];
        //need to traverse the children tree
        children = children.concat(sourceMetricParent.children);
        while (children.length > 0) {
            var child = children[0];
            //gather all metrics of all children
            if (!child.type.match(/SERVIC.*|V.*/g)) {
                sourceMetricParentChildMetrics.push(child);
            } else {
                children = children.concat(child.children);
            }

            //remove first element of the array
            children = children.slice(1);
        }
    }

    //here I have gathered all metrics from the target element children
    //now I search trough them to see which one is actually a component of the selected metric
    for (var i = 0; i < compositionRule.children.length; i++) {
        var ruleChildMetric = compositionRule.children[i];
        //for all the children's metrics
        for (var j = 0; j < sourceMetricParentChildMetrics.length; j++) {
            var childMetric = sourceMetricParentChildMetrics[j];

            //metric names in D3JS are "value [metricName]" and need to extract the metricName
            var childMetricNameIndexOfFirstBracket = childMetric.name.match(/[a-zA-Z]/);
            var childMetricNameIndexOfLastBracket = childMetric.name.match(/ \(/);
            var childMetricName = childMetric.name.slice(childMetricNameIndexOfFirstBracket.index, childMetricNameIndexOfLastBracket.index);

            //get the targeted element IDS
            targetMonitoredElementIDs = ruleChildMetric.targetMonitoredElementIDs;

            //if the metric level is the same as the clicked metric parent type
            if (ruleChildMetric.targetLevel && ruleChildMetric.targetLevel == childMetric.parent.type) {
                //if the targete lement IDs == 0 || if the target metric parent name is in the supplied IDs
                if (targetMonitoredElementIDs.length == 0 || targetMonitoredElementIDs.indexOf(childMetric.parent.name) != -1) {
                    if (ruleChildMetric.name == childMetricName) {
                        ruleChildMetrics.push(childMetric);

                        //show up to rooot (the entire composition flow)
                        highlightMetricsSources(childMetric);
                    }
                }
            }


        }
    }
    //console.log(ruleChildMetrics)
    drawMetricsLines(selectedMetric, ruleChildMetrics);

}


//returns true if it found
function buildMetricsHighlightRecursive(stack, lastMetric) {
    if (stack.length <= 0) {
        return false;
    }

    var currentElement = stack[stack.length - 1];
    if (currentElement == lastMetric) {
        return true;
    }

    var childAreTarget = false;

    if (currentElement.children) {
        for (var i = 0; i < currentElement.children.lenght; i++) {
            stack.push(currentElement.children[i]);
            if (buildMetricsHighlightRecursive(stack, lastMetric)) {
                return true;
            }
        }
        //if not return, means children are not target, so remove it
        stack.pop;
        return false;
    } else {
        stack.pop;
        return false;
    }

}

//not used
function highlightMetricsSourcesUntilSpecifiedNode(selectedMetric, lastMetric) {
    if (!metrics || !selectedMetric) {
        return
    }

    var elementsStack = [];
    elementsStack.push(selectedMetric);

    if (buildMetricsHighlightRecursive(elementsStack, lastMetric)) {
        for (var i = 1; i < elementsStack.length; i++) {
            drawMetricsLines(elementsStack[i - 1], [elementsStack[i]]);
        }
    }

}


//not used
function highlightMetricsParents(selectedMetric) {


    if (!metrics || !selectedMetric) {
        return
    }

    //get selected metric, then go trough its parents and see wtf.


    var source = selectedMetric;



    var indexOfFirstBracket = source.name.match(/[a-zA-Z]/);
    var indexOfLastBracket = source.name.match(/ \(/);
    var sourceMetricName = source.name.slice(indexOfFirstBracket.index, indexOfLastBracket.index);

    var sourceMetricParent = source.parent;

    //traverse metrics and find out what metric we have selected 
    var compositionRule;
    for (var i = 0; i < metrics.length; i++) {
        metric = metrics [i];
        if (metric.name == sourceMetricName) {
            //true even if is empty
            if (metric.targetMonitoredElementIDs) {

                //get the targeted element IDS
                targetMonitoredElementIDs = metric.targetMonitoredElementIDs;

                //if the metric level is the same as the clicked metric parent type
                if (metric.targetLevel && metric.targetLevel == sourceMetricParent.type) {

                    //if the targete lement IDs == 0 || if the target metric parent name is in the supplied IDs
                    if (targetMonitoredElementIDs.length == 0 || targetMonitoredElementIDs.indexOf(sourceMetricParent.name) != -1) {
                        compositionRule = metric;
                        break;
                    }
                }
            }
        }
    }

    if (!compositionRule) {
        return;
    }


    var sourceMetricParentChildMetrics = [];
    var ruleChildMetrics = [];

    //1'st go to the targeted element children (children of clicked sourceMetricParent)
    if (sourceMetricParent.children) {
        var children = [];
        //need to traverse the children tree
        children = children.concat(sourceMetricParent.children);
        while (children.length > 0) {
            var child = children[0];
            //gather all metrics of all children
            if (!child.type.match(/SERVIC.*|V.*/g)) {
                sourceMetricParentChildMetrics.push(child);
            } else {
                children = children.concat(child.children);
            }

            //remove first element of the array
            children = children.slice(1);
        }
    }

    //here I have gathered all metrics from the target element children
    //now I search trough them to see which one is actually a component of the selected metric
    for (var i = 0; i < compositionRule.children.length; i++) {
        var ruleChildMetric = compositionRule.children[i];
        //for all the children's metrics
        for (var j = 0; j < sourceMetricParentChildMetrics.length; j++) {
            var childMetric = sourceMetricParentChildMetrics[j];

            //metric names in D3JS are "value [metricName]" and need to extract the metricName
            var childMetricNameIndexOfFirstBracket = childMetric.name.match(/[a-zA-Z]/);
            var childMetricNameIndexOfLastBracket = childMetric.name.match(/ \(/);
            var childMetricName = childMetric.name.slice(childMetricNameIndexOfFirstBracket.index, childMetricNameIndexOfLastBracket.index);

            //get the targeted element IDS
            targetMonitoredElementIDs = ruleChildMetric.targetMonitoredElementIDs;

            //if the metric level is the same as the clicked metric parent type
            if (ruleChildMetric.targetLevel && ruleChildMetric.targetLevel == childMetric.parent.type) {
                //if the targete lement IDs == 0 || if the target metric parent name is in the supplied IDs
                if (targetMonitoredElementIDs.length == 0 || targetMonitoredElementIDs.indexOf(childMetric.parent.name) != -1) {
                    if (ruleChildMetric.name == childMetricName) {
                        ruleChildMetrics.push(childMetric);

                        //show up to rooot (the entire composition flow)
                        highlightMetricsParents(childMetric);
                    }
                }
            }


        }
    }
    //console.log(ruleChildMetrics)
    drawMetricsLines(selectedMetric, ruleChildMetrics);

}



function highlightMetricsSourcesOneLevel(selectedMetric) {
    if (!metrics || !selectedMetric) {
        return
    }


    var source = selectedMetric;

    var indexOfFirstBracket = source.name.match(/[a-zA-Z]/);
    var indexOfLastBracket = source.name.match(/ \(/);
    var sourceMetricName = source.name.slice(indexOfFirstBracket.index, indexOfLastBracket.index);

    var sourceMetricParent = source.parent;

    //traverse metrics and find out what metric we have selected 
    var compositionRule;
    for (var i = 0; i < metrics.length; i++) {
        metric = metrics [i];
        if (metric.name == sourceMetricName) {
            //true even if is empty
            if (metric.targetMonitoredElementIDs) {

                //get the targeted element IDS
                targetMonitoredElementIDs = metric.targetMonitoredElementIDs;

                //if the metric level is the same as the clicked metric parent type
                if (metric.targetLevel && metric.targetLevel == sourceMetricParent.type) {

                    //if the targete lement IDs == 0 || if the target metric parent name is in the supplied IDs
                    if (targetMonitoredElementIDs.length == 0 || targetMonitoredElementIDs.indexOf(sourceMetricParent.name) != -1) {
                        compositionRule = metric;
                        break;
                    }
                }
            }
        }
    }

    if (!compositionRule) {
        return;
    }


    var sourceMetricParentChildMetrics = [];
    var ruleChildMetrics = [];

    //1'st go to the targeted element children (children of clicked sourceMetricParent)
    if (sourceMetricParent.children) {
        var children = [];
        //need to traverse the children tree
        children = children.concat(sourceMetricParent.children);
        while (children.length > 0) {
            var child = children[0];
            //gather all metrics of all children
            if (!child.type.match(/SERVIC.*|V.*/g)) {
                sourceMetricParentChildMetrics.push(child);
            } else {
                children = children.concat(child.children);
            }

            //remove first element of the array
            children = children.slice(1);
        }
    }

    //here I have gathered all metrics from the target element children
    //now I search trough them to see which one is actually a component of the selected metric
    for (var i = 0; i < compositionRule.children.length; i++) {
        var ruleChildMetric = compositionRule.children[i];
        //for all the children's metrics
        for (var j = 0; j < sourceMetricParentChildMetrics.length; j++) {
            var childMetric = sourceMetricParentChildMetrics[j];

            //metric names in D3JS are "value [metricName]" and need to extract the metricName
            var childMetricNameIndexOfFirstBracket = childMetric.name.match(/[a-zA-Z]/);
            var childMetricNameIndexOfLastBracket = childMetric.name.match(/ \(/);
            var childMetricName = childMetric.name.slice(childMetricNameIndexOfFirstBracket.index, childMetricNameIndexOfLastBracket.index);

            //get the targeted element IDS
            targetMonitoredElementIDs = ruleChildMetric.targetMonitoredElementIDs;

            //if the metric level is the same as the clicked metric parent type
            if (ruleChildMetric.targetLevel && ruleChildMetric.targetLevel == childMetric.parent.type) {
                //if the targete lement IDs == 0 || if the target metric parent name is in the supplied IDs
                if (targetMonitoredElementIDs.length == 0 || targetMonitoredElementIDs.indexOf(childMetric.parent.name) != -1) {
                    if (ruleChildMetric.name == childMetricName) {
                        ruleChildMetrics.push(childMetric);
                    }
                }
            }


        }
    }
    //console.log(ruleChildMetrics)
    drawMetricsLines(selectedMetric, ruleChildMetrics);

}


function updateTreeView(json) {


    retrievedData = json;
    new_root = null;
    if (retrievedData.monitoringData) {
        new_root = retrievedData.monitoringData;
        metrics = retrievedData.compositionRules;

        if (fullPath) {
            highlightMetricsSources(selectedMetric);
        } else {
            highlightMetricsSourcesOneLevel(selectedMetric);
        }
    } else {
        new_root = retrievedData;
    }


//    root =  new_root;
//    update(root);
    treeRootNode = new_root;
    update(treeRootNode);


}


function openElasticitySpaceOrPathway(d) {
    if (d.type != "VM") {
        $("#mydialog").html("Please select information to display");

        $("#mydialog").dialog({
            resizable: false,
            width: 420,
            modal: true,
            buttons: {
                'Elasticity Space': function () {
                    var divID = serviceID + "_" + d.name + "_" + d.type + '_space';
                    var iframeID = serviceID + "_" + d.name + "_" + d.type + '_space_frame';

                    var iframe = document.createElement('iframe');

                    iframe.setAttribute('src', "elasticitySpace.html?" + serviceID + "&" + d.name + "&" + d.type + "&" +  spaceType);
                    iframe.setAttribute('style', 'width:100%; height:100%');
                    iframe.setAttribute('id', iframeID);

                    var frameDiv = document.createElement('div');
                    frameDiv.setAttribute('id', divID);
                    document.getElementById("analytics").appendChild(frameDiv);

                    var table = document.createElement('table');
                    frameDiv.appendChild(table);

                    var nameH = document.createElement('h2');
                    nameH.innerHTML = "Elasticity Space  for " + d.name;
                    var titleRow = document.createElement('tr');



                    var closeImage = document.createElement('img');
                    closeImage.setAttribute('src', "close.png");
                    closeImage.onclick = function () {
                        var parent = document.getElementById("analytics");
                        parent.removeChild(document.getElementById(divID));
                    };


                    var resizeImage = document.createElement('img');
                    resizeImage.setAttribute('src', "resize.png");
                    resizeImage.onclick = function () {
                        var thisFrame = document.getElementById(iframeID);
                        newheight = document.getElementById(iframeID).contentWindow.document.body.scrollHeight;
                        newwidth = document.getElementById(iframeID).contentWindow.document.body.scrollWidth;
                        thisFrame.setAttribute('style', 'width:' + newwidth + 'px; height:' + newheight + 'px');
                    };


                    var closeImageTD = document.createElement('td')
                    closeImageTD.appendChild(closeImage);

                    var resizeImageTD = document.createElement('td')
                    resizeImageTD.appendChild(resizeImage);


                    var nameHTD = document.createElement('td')
                    nameHTD.appendChild(nameH);

                    titleRow.appendChild(closeImageTD);
                    titleRow.appendChild(resizeImageTD);
                    titleRow.appendChild(nameHTD);
                    table.appendChild(titleRow);


                    frameDiv.appendChild(iframe);
                    $(this).dialog('close');
                }
                // pathway to be implemented
//                ,
//                'Elasticity Pathway': function () {
//
//
//
//                    var divID = serviceID + "_" + d.name + "_" + d.type + '_pathway';
//                    var iframeID = serviceID + "_" + d.name + "_" + d.type + '_pathway_frame';
//
//                    var iframe = document.createElement('iframe');
//
//                    iframe.setAttribute('src', "elasticityPathway.html?" + serviceID + "&" + d.name + "&" + d.type + "&" +  spaceType);
//                    iframe.setAttribute('style', 'width:100%; height:100%');
//                    iframe.setAttribute('id', iframeID);
//
//                    var frameDiv = document.createElement('div');
//                    frameDiv.setAttribute('id', divID);
//                    document.getElementById("analytics").appendChild(frameDiv);
//
//                    var table = document.createElement('table');
//                    frameDiv.appendChild(table);
//
//                    var nameH = document.createElement('h2');
//                    nameH.innerHTML = "Elasticity Pathway  for " + d.name;
//                    var titleRow = document.createElement('tr');
//
//
//
//                    var closeImage = document.createElement('img');
//                    closeImage.setAttribute('src', "close.png");
//                    closeImage.onclick = function () {
//                        var parent = document.getElementById("analytics");
//                        parent.removeChild(document.getElementById(divID));
//                    };
//
//
//                    var resizeImage = document.createElement('img');
//                    resizeImage.setAttribute('src', "resize.png");
//                    resizeImage.onclick = function () {
//                        var thisFrame = document.getElementById(iframeID);
//                        newheight = document.getElementById(iframeID).contentWindow.document.body.scrollHeight;
//                        newwidth = document.getElementById(iframeID).contentWindow.document.body.scrollWidth;
//                        thisFrame.setAttribute('style', 'width:' + newwidth + 'px; height:' + 2 * newheight + 'px');
//                    };
//
//
//                    var closeImageTD = document.createElement('td')
//                    closeImageTD.appendChild(closeImage);
//
//                    var resizeImageTD = document.createElement('td')
//                    resizeImageTD.appendChild(resizeImage);
//
//
//                    var nameHTD = document.createElement('td')
//                    nameHTD.appendChild(nameH);
//
//                    titleRow.appendChild(closeImageTD);
//                    titleRow.appendChild(resizeImageTD);
//                    titleRow.appendChild(nameHTD);
//                    table.appendChild(titleRow);
//
//
//                    frameDiv.appendChild(iframe);
//
//                    $(this).dialog('close');
//                }
//                        
            }
        });

        //var win = window.open("elasticityPathway.html?"+ serviceID + "&" + d.name + "&" + d.type, '_blank');
    }
}


