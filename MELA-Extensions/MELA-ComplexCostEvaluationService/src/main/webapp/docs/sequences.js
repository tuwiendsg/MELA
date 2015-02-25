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

//NOTICE: Work derived from D3JS examples:
//  - http://bl.ocks.org/kerryrodden/7090426
//  - http://bl.ocks.org/dbuezas/9306799
//  - http://www.jasondavies.com/coffee-wheel/



// Dimensions of sunburst.

//var elColors = ["#72B095", "#D13F31", "#D13F31"];

// var elColors = ["#1C4946", "#1F7872", "#72B095", "#DEDBA7", "#D13F31", "#8C9C9A", "#9DB2B1"]
//var elColors = ["#1C4946", "orange", "#72B095", "#DEDBA7", "#D13F31", "#8C9C9A", "#9DB2B1"]
var pieVisColors = ["orange", "#DEDBA7", "#1F7872"]

var pieVisWidth;
var pieVisHeight;
var pieVisRadius;
var pieX;
var pieY;
// Breadcrumb dimensions: width, height, spacing, width of tip/tail.
var b = {
    w: 75, h: 30, s: 3, t: 10
};


var pieFontSize = 12;
// Total size of all segments; we set this later, after loading the data.
var pieTotalSize = 0;


var pieChartSVG;
var pieChartVis;

var trailDiv;
var pieDiv;

//var analyticsDiv;

var piePartition = d3.layout.partition()
        .sort(null)
        .value(function (d) {
            return d.size;
        });


var pieArc;

function mapNodesToColor(d, selected) {
    if (selected) {
        return pieVisColors[0];
    } else {
        if (d && d.level == "metric") {
            return pieVisColors[1];
        } else {
            return pieVisColors[2];
        }
    }
}
function mapTrailElementsToColor(d) {
    return pieVisColors[2];
}

// Use d3.text and d3.csv.parseRows so that we do not need to have a header
// row, and can receive the csv as an array of arrays.
//d3.text("visit-sequences.csv", function(text) {
//  var csv = d3.csv.parseRows(text);
//  var json = buildHierarchy(csv);
//  createVisualization(json);
//});

// Main function to draw and set up the visualization, once we have the data.
function setupPieVisualization(divID, trailDivID, w, h) {//, analyticsDivID) {


    if (!trailDivID) {
        trailDiv = d3.select("#" + divID).append("div").attr("id", "trailDiv");
    } else {
        trailDiv = d3.select("#" + trailDivID);
    }
//
//    if (!analyticsDivID) {
//        analyticsDiv = d3.select("#" + divID).append("div").attr("id", "analyticsDiv");
//    } else {
//        analyticsDiv = d3.select("#" + analyticsDivID);
//    }

    b.h = Math.max(w / 20, 30);

    pieDiv = d3.select("#" + divID).append("div").attr("id", "ieDiv");

    pieVisWidth = w;
    pieVisHeight = w;

    pieVisRadius = 0.5 * Math.min(pieVisWidth, pieVisHeight) / 2;

    pieX = d3.scale.linear().range([0, 2 * Math.PI]);
    pieY = d3.scale.pow().exponent(0.9).domain([0, 1]).range([0, pieVisRadius]);

    pieArc = d3.svg.arc()
            .startAngle(function (d) {
                return Math.max(0, Math.min(2 * Math.PI, pieX(d.x)));
            })
            .endAngle(function (d) {
                return Math.max(0, Math.min(2 * Math.PI, pieX(d.x + d.dx)));
            })
            .innerRadius(function (d) {
                return Math.max(0, d.y ? pieY(d.y) : d.y);
            })
            .outerRadius(function (d) {
                return Math.max(0, pieY(d.y + d.dy));
            });

    pieChartSVG = pieDiv.append("svg:svg")
            .attr("width", pieVisWidth)
            .attr("height", pieVisHeight);


    // Basic setup of page elements.
    initializeBreadcrumbTrail(pieVisWidth);

    pieChartVis = pieChartSVG.append("svg:g")
            .attr("id", "container")
            .attr("transform", "translate(" + pieVisWidth / 2 + "," + pieVisHeight / 2 + ")");
    pieChartVis.append("svg:circle")
            .attr("r", pieVisRadius)
            .style("opacity", 0);
    pieChartVis.on("click", clear);


}

var lineFunction = d3.svg.line()
        .x(function (d) {
            return d.x;
        })
        .y(function (d) {
            return d.y;
        })
        .interpolate("linear");

function drawPieChart(json) {

    pieChartVis = pieChartSVG.append("svg:g")
            .attr("id", "container")
            .attr("transform", "translate(" + pieVisWidth / 2 + "," + pieVisHeight / 2 + ")");
    pieChartVis.append("svg:circle")
            .attr("r", pieVisRadius)
            .style("opacity", 0);
    pieChartVis.on("click", clear);

    // For efficiency, filter nodes to keep only those large enough to see.
    var nodes = piePartition.nodes(json)
            .filter(function (d) {
                return (d.dx > 0.005); // 0.005 radians = 0.29 degrees
            });
    var padding = 1;
//for each node, create arc, and add text to it as text path

    var textLines = [];
    nodes.forEach(function (d) {
        var path = pieChartVis.append("svg:path")

                .attr("d", pieArc(d))
                .attr("id", function () {
                    return "path_" + d.uniqueID;
                })
                .style("fill", function () {
                    return mapNodesToColor(d, false);
                })
                .style("stroke", function (d) {
                    return "#787878";
                })
                .on("mouseover", mouseover);

        var thing = pieChartVis.append("g")
                .attr("id", function () {
                    return "text_" + d.name;
                });

        try {
            var thingBoundingBox = path[0][0].getBBox();
        } catch (error) {
            console.log(error)
        }
        ;

        //if this is SERVICE level, then do not draw the label.
        if (d.level != "SERVICE") {


            //continue from http://bl.ocks.org/Caged/6476579 to add tooltips to small labels
            var appendedText = thing.append("text")
                    //compute width of ark to place text in middle of thickness
                    .attr("dy", function () {
                        var inside = Math.max(0, d.y ? pieY(d.y) : d.y);
                        var outsideRadius = Math.max(0, pieY(d.y + d.dy));
                        var thickness = outsideRadius - inside;
                        return thickness / 2; //last1 0 is half of font size
                    })
                    .style("font-size", pieFontSize + "px")
                    .append("textPath")

                    .attr("xlink:href", function () {
                        return "#path_" + d.uniqueID;
                    })
                    //place text towards middle of arc
                    .attr("startOffset", "20%")
                    .attr("text-anchor", "middle")

                    .text(function () {
                        if (thingBoundingBox) {
                            boxWidth = Math.max(thingBoundingBox.width, thingBoundingBox.height);
                            if (boxWidth >= d.name.length * pieFontSize / 2) {
                                return d.name;
                            } else {
                                var splitMargin = (boxWidth - 3) / (pieFontSize * 2 / 3)
                                return d.name.substring(0, splitMargin) + "...";
                            }
                        } else {
                            return d.name;
                        }
                    })
                    ;



            var startAngle = Math.max(0, Math.min(2 * Math.PI, pieX(d.x)));

            var endAngle = Math.max(0, Math.min(2 * Math.PI, pieX(d.x + d.dx)));


            var outmostNode = nodes.slice(0).sort(function (a, b) {
                return a.depth > b.depth;
            })[0];

            var outsideRadius = Math.max(0, pieY(outmostNode.y + outmostNode.dy));

            var arcRadius = Math.max(0, pieY(d.y + d.dy));


//        var middleOfArc = d3.interpolateNumber(a, b);

            //text is placed on outsidemost ring

            var parentTextpath = pieDiv.select("#path_" + d.uniqueID)[0][0];
            var parentTextPathMiddlePoint = parentTextpath.getPointAtLength(parentTextpath.getTotalLength() / 3);

            var textStartPoint = {x: 0, y: 0};
            var arcStartPoint = {x: 0, y: 0};
            var arcEndPoint = {x: 0, y: 0};

            textStartPoint.x = outsideRadius;

            textStartPoint.y = parentTextPathMiddlePoint.y;

            arcStartPoint.x = arcRadius * Math.cos(startAngle) - 10;
            arcStartPoint.y = arcRadius * Math.sin(startAngle);

            arcEndPoint.x = arcRadius * Math.cos(endAngle);
            arcEndPoint.y = arcRadius * Math.sin(endAngle);


            if (parentTextPathMiddlePoint.x <= 0) {
                textStartPoint.x = -outsideRadius;
                arcStartPoint.x = -arcStartPoint.x;

            } else {
                textStartPoint.x = outsideRadius;
            }

            var arcCenter = {x: arcStartPoint.x, y: textStartPoint.y};
            arcStartPoint = {x: parentTextPathMiddlePoint.x, y: parentTextPathMiddlePoint.y};


            //if no space to put text, put text near the drawing and draw a line to it
            if (thingBoundingBox) {
                boxWidth = Math.max(thingBoundingBox.width, thingBoundingBox.height);
                if (boxWidth <= d.name.length * pieFontSize * 2 / 3) {

                    //append line from text to its arc
                    var lineData = [{x: textStartPoint.x, y: textStartPoint.y}, {x: arcStartPoint.x, y: arcStartPoint.y}];
                    textLines.push({text: d.name, line: lineData});
                }
            }
        }

    });


    //append text
//                thing.append("text")
//                        .attr("dx", function () {
//                            return textStartPoint.x;
//                        })
//                        .attr("dy", function () {
//                            return textStartPoint.y;
//                        }).attr("anchor", "left")
//                        .text(d.name);

    //if outer text labels overlap each other, we must redistribute them
    //if in same side of the pie, move them to have on vertical at least 2 font sizes
    //so first we split ones in left, and right, and rearrange them


    var leftSizeUpperLines = textLines
            .filter(function (l) {
                return l.line[0].x < 0 && l.line[0].y < 0;
            })
            .sort(function (a, b) {
                return a.line[0].y < b.line[0].y;
            });

    var leftSizeLowerLines = textLines.filter(function (l) {
        return l.line[0].x < 0 && l.line[0].y >= 0;
    });

    var rightSizeLinesUpper = textLines
            .filter(function (l) {
                return l.line[0].x >= 0 && l.line[0].y < 0;
            })
            .sort(function (a, b) {
                return a.line[0].y < b.line[0].y;
            });
    ;

    var rightSizeLinesLower = textLines.filter(function (l) {
        return l.line[0].x >= 0 && l.line[0].y >= 0;
    });

    shiftOverlappingCoordinates(leftSizeUpperLines, -pieFontSize);
    shiftOverlappingCoordinates(leftSizeLowerLines, pieFontSize);

    shiftOverlappingCoordinates(rightSizeLinesUpper, -pieFontSize);
    shiftOverlappingCoordinates(rightSizeLinesLower, pieFontSize);


    textLines = textLines.sort(function (a, b) {
        if (a.line[0].y > 0 && b.line[0].y) {
            return a.line[0].y < b.line[0].y;
        } else {
            return a.line[0].y > b.line[0].y;
        }
    });

    if (textLines.length > 0) {
//        var lastElIndex = textLines.length - 1;
//        var textWidth = textLines[lastElIndex].text.length * pieFontSize / 2;
//        var newLineData = [textLines[lastElIndex].line[0], {x: textLines[lastElIndex].line[0].x + textWidth, y: textLines[lastElIndex].line[0].y}, textLines[lastElIndex].line[1]];
//        textLines[lastElIndex].line = newLineData;


        textLines.forEach(function (d) {
            pieChartVis.append("text")
                    .attr("dx", function () {
                        return d.line[0].x;
                    })
                    .attr("dy", function () {
                        return d.line[0].y;
                    })
                    .attr("text-anchor", function () {
                        if (d.line[0].x <= 0) {
                            return "end";
                        } else {
                            return "start";
                        }
                    })
                    .text(d.text)
                    .style("font-size", pieFontSize + "px")
                    ;

//        //avoid underlining text
//        if(d.line[0].x < 0){
//            d.line = [d.line[1],d.line[2]];
//        }
            pieChartVis.append("path").attr("d", lineFunction(d.line))
                    .attr("stroke", "black")
                    .attr("stroke-width", 1)
                    .attr("fill", "none");
            ;
        });
    }

//has some needed side effects and the breadcrum/selection does not seem to work without
    pieChartSVG.selectAll("path").data(nodes);
// Get total size of the tree = value of root node from partition.
    pieTotalSize = json.value;
}


/**
 * 
 * @param {type} lines lines to shift if they overlap
 * @param {shiftAmount} shiftSign if the shift should be done back (if shiftAmount is negative)
 * @returns {undefined}
 */
function shiftOverlappingCoordinates(lines, shiftAmount) {

    for (var i = 0; i < lines.length - 1; i++) {
        var current = lines[i].line;
        var next = lines[i + 1].line;
        //try to keep a font size between them
        //if both x coord have same sign

        if (Math.abs(next[0].y) < Math.abs(current[0].y + shiftAmount)) {
            next[0].y = next[0].y + shiftAmount;
            for (var j = i + 2; j < lines.length; j++) {
                lines[j].line[0].y = lines[j].line[0].y + shiftAmount;
            }
        }

    }


}

function updatePieVisualization(json) {

    pieChartVis.remove();
    drawPieChart(json);
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

//        pieChartVis.selectAll("path")
//
//                .filter(function (d) {
//                    return d;
//                })
//                //.style("opacity", 1)
//                .style("fill", function (d) {
//                    return mapNodesToColor(d, true);
//                })
//                .style("stroke", function (d) {
//                    return "#787878";
//                });
     
       mouseover(nodeInUpdatedStructure);
       
    }
    
    


}
// Fade all but the current sequence, and show it in the breadcrumb trail.
function clear() {

    if (treeVisualization) {
        treeVisualization.selectAll("path").filter(function (node) {
            var nodeToShade = this;
            if (node.level && node.level == "metric") {
                if (node.category == "COST") {
                    nodeToShade.style.fill = "#FFE773";
                } else {
                    nodeToShade.style.fill = "gray";
                }
            }


        });
    }
    d3.select("#percentage")
            .text("");
    d3.select("#explanation")
            .style("visibility", "");
    // Data join; key function combines name and depth (= position in sequence).
    var g = d3.select("#trail")
            .selectAll("g");
    g.remove();
    var percentageText = d3.select("#trail")
            .selectAll("#endlabel");
    percentageText.text("");
    // Fade all the segments.

    pieDiv.selectAll("path")
            .filter(function (d) {
                return d;
            })
            .style("fill", function (d) {
                return mapNodesToColor(d, false);
            })
            .style("stroke", function (d) {
                return "#787878";
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
    pieDiv.selectAll("path")
            //exclude paths which have no data attached, such as paths freom arc to text label
            .filter(function (d) {
                return d;
            })
            //.style("opacity", 0.3)
            .style("fill", function (d) {
                return mapNodesToColor(d, false);
            })

            ;
    // Then highlight only those that are an ancestor of the current segment.
    pieChartVis.selectAll("path")
            .filter(function (node) {
                return (sequenceArray.indexOf(node) >= 0);
            })
            //.style("opacity", 1)
            .style("fill", function (d) {
                return mapNodesToColor(d, true);
            });
//
//    // Then highlight only those that are an ancestor of the current segment.
//    treeVisualization.selectAll("path")
//
//            .style("fill", function (d) {
//                if (d.type == "metric") {
//                    return "gray";
//                } else {
//                    if (d.attention) {
//                        return "#D13F31";
//                    } else {
//                        if (d.type == "SERVICE" || d.type == "SERVICE_TOPOLOGY" || d.type == "SERVICE_UNIT") {
//                            return "#CCFFFF";
//                        } else {
//                            return "black";
//                        }
//                    }
//                }
//            }
//            );


//highlight selected path in tree vis
    treeVisualization.selectAll("path").filter(function (node) {
        var nodeToShade = this;
        //when I highlight metric sources, i generate paths with no source nodes
        if (node) {
            //smt interesting hapends
            //d3js returns my node "d" such as "metric with name etc", and as this the "path"
            //so I use the node to check attributes, and the path to color

            if (node.type && node.type == "metric") {
                if (node.category == "COST") {
                    nodeToShade.style.fill = "#FFE773";
                } else {
                    nodeToShade.style.fill = "gray";
                }

            }
        }
    });

    var metricsToLink = [];
    treeVisualization.selectAll("path").filter(function (node) {
        var nodeToShade = this;
        sequenceArray.forEach(function (ancestorNode) {

//            if (node.type && node.type != "metric") {
//                if (node.name && node.name == ancestorNode.name
//                        && ancestorNode.type == node.level
//                        && node.parent.name == ancestorNode.parent.name
//                        && ancestorNode.parent.type == node.parent.level
//                        ) {
////                    nodeToShade.style.fill = "red";
//                }
//            } else 
            if (node) {
                if (node.type && node.type == "metric") {
                    if (node.name && node.name.indexOf(ancestorNode.name) > -1
                            && ancestorNode.type == node.level
                            && node.parent.name == ancestorNode.parent.name
                            && ancestorNode.parent.type == node.parent.level
                            ) {
                        nodeToShade.style.fill = "orange";
                        metricsToLink.push(node);
                    }
                }
            }
        });
    });

//    treeVisualization.selectAll("path.metricLink").remove();

//    //I need to go reverse and highloght all parents
//    var lastMetric = metricsToLink[metricsToLink.length - 1];
//
//    highlightMetricsSourcesUntilSpecifiedNode(metricsToLink[0], lastMetric);
////
//    for (var i = 1; i < metricsToLink.length; i++) {
//        drawMetricsLines(metricsToLink[i - 1], [metricsToLink[i]]);
//    }


//
//    sequenceArray.forEach(function (ancestor) {
//        nodes.forEach(function (node) {
//            if (node.name.indexOf(ancestor.name) > -1
//                    && node.type == ancestor.level
//                    && node.parent.name.indexOf(ancestor.parent.name) > -1
//                    && node.parent.type == ancestor.parent.level
//                    ) {
//
//                treeVisualization.selectAll("path").filter(function (n) {
//                    if (n.__data__) {
//                        return n.__data__ == node;
//                    } else {
//                        return false;
//                    }
//                }).style("fill", function () {
//                    return "red";
//                });
//            }
//
//        });
//
//    });


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
    path.unshift(current);
    while (current.parent) {
        current = current.parent;
        path.unshift(current);
    }
    return path;
}

function initializeBreadcrumbTrail(width) {

    var oldTrail = document.getElementById("trail");
    if (oldTrail)
    {
        oldTrail.remove();
    }
    // Add the svg area.
    var trail = trailDiv
            .append("svg:svg")
            .attr("width", width)
            .attr("height", b.h)
            .attr("id", "trail");
    // Add the label at the end, for the percentage.
    trail.append("svg:text")
            .attr("id", "endlabel")
            .style("fill", "#000").style("font-size", pieFontSize + "px");
}

var trailPointsWidth = [];
// Generate a string that describes the points of a breadcrumb polygon.
function breadcrumbPoints(d, i) {
    var nameSize = Math.max(d.name.length * pieFontSize * 2 / 3, b.w);
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
    var computedNeededWidth = trailPointsWidth[trailPointsWidth.length - 1] + percentageString.length * pieFontSize * 2 / 3 + 2 * b.h;
    if (computedNeededWidth > pieVisWidth) {
        initializeBreadcrumbTrail(computedNeededWidth);
    }

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
                var nameSize = d.name.length * pieFontSize * 2 / 3;
                return Math.max((nameSize + b.t) / 2, b.w / 2);
            })
            .attr("y", b.h / 2)
            .attr("dy", "0.35em")
            .style("font-size", pieFontSize + "px")
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
    trailDiv.select("#endlabel")
            .attr("x", function (d) {
                return(trailPointsWidth[nodeArray.length - 1] + 2 * b.h);
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

