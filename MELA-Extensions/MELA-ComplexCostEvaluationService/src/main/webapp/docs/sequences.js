//Copyright 2013 Technische Universitat Wien (TUW), Distributed Systems Group  E184
//
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.

//NOTICE: Work derived from Sun-Burst D3_JS example provided by http://bl.ocks.org/kerryrodden/7090426


// Dimensions of sunburst.

//var elColors = ["#72B095", "#D13F31", "#D13F31"];

// var elColors = ["#1C4946", "#1F7872", "#72B095", "#DEDBA7", "#D13F31", "#8C9C9A", "#9DB2B1"]
var elColors = ["#1C4946", "orange", "#72B095", "#DEDBA7", "#D13F31", "#8C9C9A", "#9DB2B1"]

var width = window.innerWidth - 100;
var height = window.innerHeight - 100;
var radius = Math.min(width, height) / 2;
var x = d3.scale.linear().range([0, 2 * Math.PI]);
var y = d3.scale.pow().exponent(0.9).domain([0, 1]).range([0, radius]);
// Breadcrumb dimensions: width, height, spacing, width of tip/tail.
var b = {
    w: 75, h: 30, s: 3, t: 10
};


var fontSize = 20;
// Total size of all segments; we set this later, after loading the data.
var totalSize = 0;


var svg;
var vis;

var partition = d3.layout.partition()
        .sort(null)
        .value(function (d) {
            return 5.8 - d.depth;
        });

var arc = d3.svg.arc()
        .startAngle(function (d) {
            return Math.max(0, Math.min(2 * Math.PI, x(d.x)));
        })
        .endAngle(function (d) {
            return Math.max(0, Math.min(2 * Math.PI, x(d.x + d.dx)));
        })
        .innerRadius(function (d) {
            return Math.max(0, d.y ? y(d.y) : d.y);
        })
        .outerRadius(function (d) {
            return Math.max(0, y(d.y + d.dy));
        });

function mapNodesToColor(d, selected) {
    if (selected) {
        return elColors[1];
    } else {
        return elColors[2];
    }
}
function mapTrailElementsToColor(d) {
    return elColors[0];
}

// Use d3.text and d3.csv.parseRows so that we do not need to have a header
// row, and can receive the csv as an array of arrays.
//d3.text("visit-sequences.csv", function(text) {
//  var csv = d3.csv.parseRows(text);
//  var json = buildHierarchy(csv);
//  createVisualization(json);
//});

// Main function to draw and set up the visualization, once we have the data.
function createVisualization(json) {

    svg = d3.select("#chart").append("svg:svg")
            .attr("width", width)
            .attr("height", height);

    vis = svg.append("svg:g")
            .attr("id", "container")
            .attr("transform", "translate(" + width / 2 + "," + height / 2 + ")");

    // Basic setup of page elements.
    initializeBreadcrumbTrail();
//    drawLegend();
//    d3.select("#togglelegend").on("click", toggleLegend);

    // Bounding circle underneath the sunburst, to make it easier to detect
    // when the mouse leaves the parent g.
    vis.append("svg:circle")
            .attr("r", radius)
            .style("opacity", 0);

    vis.on("click", clear);

    // For efficiency, filter nodes to keep only those large enough to see.
    var nodes = partition.nodes(json)
            .filter(function (d) {
                return (d.dx > 0.005); // 0.005 radians = 0.29 degrees
            });

    var padding = 1;

//for each node, create arc, and add text to it as text path
    nodes.forEach(function (d) {
        var path = vis.append("svg:path")
                .attr("display", function () {
                    return d.depth ? null : "none";
                })
                .attr("d", arc(d))
                .attr("id", function () {
                    return "path_" + d.name;
                })
                .style("fill", function () {
                    return mapNodesToColor(d, false);
                })
                //.style("opacity", 1)
                .on("mouseover", mouseover);

        var thing = vis.append("g")
                .attr("id", function () {
                    return "text_" + d.name;
                });
//                .style("fill", "navy");

        thing.append("text")
                //compute width of ark to place text in middle of thickness
                .attr("dy", function () {
                    var inside = Math.max(0, d.y ? y(d.y) : d.y);
                    var outsideRadius = Math.max(0, y(d.y + d.dy));

                    var thickness = outsideRadius - inside;
                    return thickness / 2; //last1 0 is half of font size
                })
                .style("font-size", fontSize + "px")
                .append("textPath")

                .attr("xlink:href", function () {
                    return "#path_" + d.name;
                })
                //place text towards middle of arc
                .attr("startOffset", "20%")
                .attr("text-anchor", "middle")

                .text(function () {
                    return d.name;
                });

    });


//has some needed side effects and the breadcrum/selection does not seem to work without
    svg.selectAll("path").data(nodes);


    var root = d3.layout.tree().nodes(json).reverse();

    // Get total size of the tree = value of root node from partition.
    totalSize = root.value;

}
function recreateVisualization(json) {

    svg.remove();
    createVisualization(json);
    if (highlightedNode) {
        //need to find the updated node in current structure

        var nodesList = d3.layout.tree().nodes(json).reverse();

        var oldNodeAncestors = getAncestors(highlightedNode);

        //as struct might update, we need to check up to which node we highlight
        var nodeInUpdatedStructure;

        while (!nodeInUpdatedStructure && oldNodeAncestors.length > 0) {

            var currentTestedNode = oldNodeAncestors.pop();
            for (i = 0; i < nodesList.length; i++) {
                var currentNode = nodesList[i];
                if (currentNode.name === currentTestedNode.name) {
                    nodeInUpdatedStructure = currentNode;
                    break;
                }
            }
        }

        if (!nodeInUpdatedStructure) {
            return;

        }

        var ancestors = getAncestors(nodeInUpdatedStructure);

        updateBreadcrumbs(ancestors, nodeInUpdatedStructure.size);
        //old unselected nodes need to be made with less opacity
        d3.selectAll("path")
                //.style("opacity", 0.3)
                .style("fill", function (d) {
                    return mapNodesToColor(d, false);
                });

        vis.selectAll("path")
                .filter(function (node) {
                    return (ancestors.indexOf(node) >= 0);
                })
                //.style("opacity", 1)
                .style("fill", function (d) {
                    return mapNodesToColor(d, true);
                });

    }


}
// Fade all but the current sequence, and show it in the breadcrumb trail.
function clear() {


    d3.select("#percentage")
            .text("");

    d3.select("#explanation")
            .style("visibility", "");


    // Data join; key function combines name and depth (= position in sequence).
    var g = d3.select("#trail")
            .selectAll("g");
    g.remove();


    // Fade all the segments.
    d3.selectAll("path")
            //.style("opacity", 1)
            .style("fill", function (d) {
                return mapNodesToColor(d, false);
            });
    ;

}

var highlightedNode;

// Fade all but the current sequence, and show it in the breadcrumb trail.
function mouseover(d) {

    highlightedNode = d;

//  var percentage = (100 * d.value / totalSize).toPrecision(3);
    var percentage = d.displayValue;
    var percentageString = percentage;
    if (percentage < 0.1) {
        percentageString = "< 0.1%";
    }

    d3.select("#percentage")
            .text(percentageString);

    d3.select("#explanation")
            .style("visibility", "");

    var sequenceArray = getAncestors(d);
    updateBreadcrumbs(sequenceArray, percentageString);

    // Fade all the segments.
    d3.selectAll("path")
            //.style("opacity", 0.3)
            .style("fill", function (d) {
                return mapNodesToColor(d, false);
            });

    // Then highlight only those that are an ancestor of the current segment.
    vis.selectAll("path")
            .filter(function (node) {
                return (sequenceArray.indexOf(node) >= 0);
            })
            //.style("opacity", 1)
            .style("fill", function (d) {
                return mapNodesToColor(d, true);
            });
}

// Restore everything to full opacity when moving off the visualization.
function mouseleave(d) {

    highlightedNode = null;

    // Hide the breadcrumb trail
    d3.select("#trail")
            .style("visibility", "hidden");

    // Deactivate all segments during transition.
    d3.selectAll("path").on("mouseover", null);

    // Transition each segment to full opacity and then reactivate it.
    d3.selectAll("path")
            .transition()
            .duration(1000)
            //.style("opacity", 1)
            .each("end", function () {
                d3.select(this).on("mouseover", mouseover);
            });

    d3.select("#explanation")
            .style("visibility", "hidden");
}

// Given a node in a partition layout, return an array of all of its ancestor
// nodes, highest first, but excluding the root.
function getAncestors(node) {
    var path = [];
    var current = node;
    while (current.parent) {
        path.unshift(current);
        current = current.parent;
    }
    return path;
}

function initializeBreadcrumbTrail() {
    // Add the svg area.
    var trail = d3.select("#sequence").append("svg:svg")
            .attr("width", width)
            .attr("height", 50)
            .attr("id", "trail");
    // Add the label at the end, for the percentage.
    trail.append("svg:text")
            .attr("id", "endlabel")
            .style("fill", "#000");
}

var trailPointsWidth = [];
// Generate a string that describes the points of a breadcrumb polygon.
function breadcrumbPoints(d, i) {
    var nameSize = Math.max(d.name.length * fontSize / 2, b.w);
    if (i > 0 && trailPointsWidth[i - 1]) {
        trailPointsWidth[i] = (nameSize + trailPointsWidth[i - 1] + b.s);
    } else {
        trailPointsWidth[i] = nameSize;
    }
    var points = [];
    points.push("0,0");
    points.push(nameSize + ",0");
    points.push(nameSize + b.t + "," + (b.h / 2));
    points.push(nameSize + "," + b.h);
    points.push("0," + b.h);
    if (i > 0) { // Leftmost breadcrumb; don't include 6th vertex.
        points.push(b.t + "," + (b.h / 2));
    }
    return points.join(" ");
}

// Update the breadcrumb trail to show the current sequence and percentage.
function updateBreadcrumbs(nodeArray, percentageString) {



    // Data join; key function combines name and depth (= position in sequence).
    var g = d3.select("#trail")
            .selectAll("g")
            .data(nodeArray, function (d) {
                return d.name + d.depth;
            });

    // Add breadcrumb and label for entering nodes.
    var entering = g.enter().append("svg:g");

    entering.append("svg:polygon")
            .attr("points", breadcrumbPoints)
            .style("fill", function (d) {
                return mapTrailElementsToColor(d);
            });


    entering.append("svg:text")
            .attr("x", function (d) {
                var nameSize = d.name.length * fontSize / 2;
                return Math.max((nameSize + b.t) / 2, b.w / 2);
            })
            .attr("y", b.h / 2)
            .attr("dy", "0.35em")
            .attr("text-anchor", "middle")
            .text(function (d) {
                return d.name;
            });

    // Set position for entering and updating nodes.
    g.attr("transform", function (d, i) {
        if (i > 0) {
            var prevTrailWidth = trailPointsWidth[i - 1];
            return "translate(" + (prevTrailWidth + b.s) + ", 0)";
        } else {
            return "translate(0, 0)";
        }

    });

    // Remove exiting nodes.
    g.exit().remove();

    // Now move and update the percentage at the end.
    d3.select("#trail").select("#endlabel")
            .attr("x", function (d) {
                return(trailPointsWidth[nodeArray.length - 1] + b.h);
            })
            .attr("y", b.h / 2)
            .attr("dy", "0.35em")
            .attr("text-anchor", "middle")
            .text(percentageString);

    // Make the breadcrumb trail visible, if it's hidden.
    d3.select("#trail")
            .style("visibility", "");

}
//
//function drawLegend() {
//
//    // Dimensions of legend item: width, height, spacing, radius of rounded rect.
//    var li = {
//        w: 75, h: 30, s: 3, r: 3
//    };
//
//    var legend = d3.select("#legend").append("svg:svg")
//            .attr("width", li.w)
//            .attr("height", d3.keys(colors).length * (li.h + li.s));
//
//    var g = legend.selectAll("g")
//            .data(d3.entries(colors))
//            .enter().append("svg:g")
//            .attr("transform", function (d, i) {
//                return "translate(0," + i * (li.h + li.s) + ")";
//            });
//
//    g.append("svg:rect")
//            .attr("rx", li.r)
//            .attr("ry", li.r)
//            .attr("width", li.w)
//            .attr("height", li.h)
//            .style("fill", function (d) {
//                return d.value;
//            });
//
//    g.append("svg:text")
//            .attr("x", li.w / 2)
//            .attr("y", li.h / 2)
//            .attr("dy", "0.35em")
//            .attr("text-anchor", "middle")
//            .text(function (d) {
//                return d.key;
//            });
//}
//
//function toggleLegend() {
//    var legend = d3.select("#legend");
//    if (legend.style("visibility") == "hidden") {
//        legend.style("visibility", "");
//    } else {
//        legend.style("visibility", "hidden");
//    }
//}
 