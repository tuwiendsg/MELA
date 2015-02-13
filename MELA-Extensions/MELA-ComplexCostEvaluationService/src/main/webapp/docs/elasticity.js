var margin = {top: 20, right: 20, bottom: 30, left: 120},
width = 500 - margin.left - margin.right,
        height = 270 - margin.top - margin.bottom;
var opts = {
    lines: 20, // The number of lines to draw
    length: 7, // The length of each line
    width: 10, // The line thickness
    radius: 100, // The radius of the inner circle
    corners: 1, // Corner roundness (0..1)
    rotate: 0, // The rotation offset
    color: '#000', // #rgb or #rrggbb
    speed: 1, // Rounds per second
    trail: 60, // Afterglow percentage
    shadow: false, // Whether to render a shadow
    hwaccel: false, // Whether to use hardware acceleration
    className: 'spinner', // The CSS class to assign to the spinner
    zIndex: 2e9, // The z-index (defaults to 2000000000)
    top: 'auto', // Top position relative to parent in px
    left: 'auto', // Left position relative to parent in px
    visibility: true
};
var colorScheme = ["#1C4946", "#1F7872", "#72B095", "#DEDBA7", "#D13F31", "#8C9C9A", "#9DB2B1"]

var loadingSpinner = new Spinner(opts);
function generateElSpaceChart(elSpaceJSON, rootNode, lineThickness, lineColors, xLoc, yLoc, width, height, xAxisLabel) {



    var maxY;
    var minY = 0;
    for (var dimensionIndex = 0; dimensionIndex < elSpaceJSON.dimensions.length; dimensionIndex++) {


        var dimension = elSpaceJSON.dimensions[dimensionIndex];
        var index = 1;
        for (var valueIndex = 0; valueIndex < dimension.values.length; valueIndex++) {
            dimension.values[valueIndex].index = index;
            index = index + 1;
        }

        var maxValue = Math.max.apply(null, dimension.values.map(function (c) {
            return c.value;
        }));
//        var minValue = Math.min.apply(null, dimension.values.map(function (c) {
//            return c.value;
//        }));


        if (!maxY || maxY < maxValue) {
            maxY = maxValue;
        }
//
//
//        if (!minY || minY > minValue) {
//            minY = minValue;
//        }

    }


    var x = d3.scale.linear()
            .range([xLoc, xLoc + width]);
    var y = d3.scale.linear()
            .range([yLoc + height, yLoc]);
    var xAxis = d3.svg.axis()
            .scale(x)
            .orient("bottom");
    var yAxis = d3.svg.axis()
            .scale(y)
            .orient("left");
    x.domain(d3.extent(elSpaceJSON.dimensions[0].values, function (d) {
        return d.index;
    }));
    y.domain([minY, maxY]);

    //create sorounding rect
    elSpaceSVG.append("rect")
            .attr("x", xLoc)
            .attr("y", yLoc)
            .attr("width", width)
            .attr("height", height)
            .style("fill", "none").style("stroke", "black");

    //create axis
//  //  x axis
//    elSpaceSVG.append("g")
//            .attr("class", "x axis")
//            .attr("x", xLoc)
//            .style("font-size", "140%")
//            .attr("transform", "translate(0," + (+yLoc + +height) + ")")
//            //                .transition().duration(1000)
//            .call(xAxis)
//            .selectAll("text")
//            .style("text-anchor", "end")
//            .attr("dx", "-.8em")
//            .attr("dy", ".15em")
//            .attr("transform", function (d) {
//                return "rotate(-65)"
//            });

//    elSpaceSVG.append("g")
//            .attr("class", "y axis")
//            .append("text")
//            .attr("y", +yLoc + +height + 60)
//            .attr("x", +xLoc + +width / 2 - "Time(s)".length * 6 / 2)
//            .attr("dy", ".71em")
//            .style("text-anchor", "start")
//            .style("font-size", "200%")
//            .text("Time(s)");

  // y axis

    elSpaceSVG.append("g")
            .attr("class", "y axis")
            .attr("transform", "translate(" + xLoc + ",0)")
            .style("font-size", "140%")
            .call(yAxis)
            ;


    elSpaceSVG.append("g")
            .attr("class", "y axis").append("text")
            .attr("transform", "rotate(-90)")
            .attr("y", xLoc - 70)
            .attr("x", -yLoc - +height / 2 - "Cost (units)".length * 4)
            .attr("dy", ".71em")
            .style("text-anchor", "start")
            .style("font-size", "180%")
            .text("Cost (units)");


    var line = d3.svg.line()
            .x(function (d) {
                return x(d.index);
            })
            .y(function (d) {
                return y(d.value);
            });
            
    for (var dimensionIndex = 0; dimensionIndex < elSpaceJSON.dimensions.length; dimensionIndex++) {


        var dimension = elSpaceJSON.dimensions[dimensionIndex];
        elSpaceSVG.append("g").append("path")
                .datum(dimension.values)
                .style("stroke", function () {
                    if (dimensionIndex > colorScheme.length - 1) {
                        return colorScheme[dimensionIndex % colorScheme.length];
                    } else {
                        return colorScheme[dimensionIndex];
                    }
                })
                .style("stroke-width", "2")
                .attr("d", line);
        
        elSpaceSVG.append("text")
                .style("font-size", "140%")
                .attr("transform", function () {
                    lastValue = dimension.values[dimension.values.length - 1];
                    return "translate(" + x(lastValue.index) + "," + y(lastValue.value) + ")";
                })
//                .attr("x", function () {
//                    lastValue = dimension.values[dimension.values.length - 1];
//                    x(lastValue.index);
//                })
                .attr("dy", ".35em")
                .text(function () {
                    return dimension.name;
                });
    }

//    var currentColumn = 0;
//    var currentRow = 0;
//
//    var dimensionNames = [];
//    for (var dimensionIndex = 0; dimensionIndex < elSpaceJSON.dimensions.length; dimensionIndex++) {
//        if (currentColumn >= maxColumns) {
//            currentColumn = 0;
//            currentRow = currentRow + 1;
//        }
//        xLocation = xLoc + currentColumn * (width + 100);
//        yLocation = yLoc + currentRow * (width - 20);
//
//        var x = d3.scale.linear()
//                .range([xLocation, xLocation + width]);
//
//        var y = d3.scale.linear()
//                .range([yLocation + height, yLocation]);
//
//        var xAxis = d3.svg.axis()
//                .scale(x)
//                .orient("bottom");
//
//        var yAxis = d3.svg.axis()
//                .scale(y)
//                .orient("left");
//
//        dimension = elSpaceJSON.dimensions[dimensionIndex];
//
//        var index = 1;
//
//
//        for (var valueIndex = 0; valueIndex < dimension.values.length; valueIndex++) {
//            dimension.values[valueIndex].index = index * 10;
//            dimension.upperBoundary[valueIndex].index = index * 10;
//            dimension.lowerBoundary[valueIndex].index = index * 10;
//            index = index + 1;
//        }
//
//        x.domain(d3.extent(dimension.values, function (d) {
//            return d.index;
//        }));
//
//        var minY = dimension.lowerBoundary[0].value;
//
//
//        var maxValue = Math.max.apply(null, dimension.values.map(function (c) {
//            return c.value;
//        }));
//
//        var minValue = Math.min.apply(null, dimension.values.map(function (c) {
//            return c.value;
//        }));
//
//
//        var maxY = dimension.upperBoundary[0].value;
//
//        if (minY == maxY) {
//            if (minY != 0) {
//                minY = 0;
//            } else {
//                maxY = 1;
//            }
//        }
//
//        if (maxY < maxValue) {
//            maxY = maxValue;
//        }
//
//        if (minY > minValue) {
//            minY = minValue;
//        }
//
//        y.domain([minY, maxY]);
//
//        var yAxisLabel = dimension.name + " (" + dimension.unit + ")";
//
//        dimensionNames.push(yAxisLabel);
//
//        rootNode.append("rect")
//                .attr("x", xLocation)
//                .attr("y", yLocation)
//                .attr("width", width)
//                .attr("height", height)
//                .style("fill", "none").style("stroke", "black");
//
//
//        var path = rootNode.append("g")
//                .attr("class", "y axis")
//                .attr("x", xLocation)
//                .style("font-size", "140%")
//                .attr("transform", "translate(0," + (+yLocation + +height) + ")")
//                //                .transition().duration(1000)
//                .call(xAxis)
//                .selectAll("text")
//                .style("text-anchor", "end")
//                .attr("dx", "-.8em")
//                .attr("dy", ".15em")
//                .attr("transform", function (d) {
//                    return "rotate(-65)"
//                });
//
//        //
//        //                path.transition()
//        //                .duration(1000)
//        //                .ease("linear");
//
//        rootNode.append("g")
//                .attr("class", "y axis")
//                .attr("transform", "translate(" + xLocation + ",0)")
//                .style("font-size", "140%")
//                .call(yAxis)
//
//
//        rootNode.append("g")
//                .attr("class", "y axis").append("text")
//                .attr("transform", "rotate(-90)")
//                .attr("y", xLocation - 70)
//                .attr("x", -yLocation - +height / 2 - yAxisLabel.length * 4)
//                .attr("dy", ".71em")
//                .style("text-anchor", "start")
//                .style("font-size", "180%")
//                .text(yAxisLabel);
//
//        rootNode.append("g")
//                .attr("class", "y axis")
//                .append("text")
//                .attr("y", +yLocation + +height + 60)
//                .attr("x", +xLocation + +width / 2 - xAxisLabel.length * 6 / 2)
//                .attr("dy", ".71em")
//                .style("text-anchor", "start")
//                .style("font-size", "200%")
//                .text(xAxisLabel);
//
//
//
//
//
//
////        rootNode.append("path")
////                .datum(dimension.upperBoundary)
////                .attr("class", "line")
////                .style("stroke", lineColors[1])
////                .style("stroke-width", lineThickness[1])
////                .attr("d", line);
////
////        rootNode.append("path")
////                .datum(dimension.lowerBoundary)
////                .attr("class", "line")
////                .style("stroke", lineColors[2])
////                .style("stroke-width", lineThickness[2])
////                .attr("d", line);
//
//
//
//
//
////                var legendNames = ["Upper Boundary", "Dimension: " + dimension.name, "Lower Boundary" ];
//        var legendNames = ["Upper Boundary", dimension.name, "Determined Boundaries"];
//
////                //upper boundary legend entry
////                {
////                    var legend = rootNode                 
////                    .append("g")
////                    .data([legendNames[0]])
////                    .attr("class", "legend")
////                    .attr("transform", function(d, i) { return "translate("  + 100 + "," + (+yLocation -20 )+ ")"; });
////
////                    legend.append("rect")
////                    .attr("x", xLocation - 18)
////                    .attr("y", -5)
////                    .attr("width", 10)
////                    .attr("height", 10)
////                    .style("fill", function(d){
////                        if(d.match(/.*Boundary/)){
////                            return "D13F31";
////                        }else{
////                            return "72B095";
////                        }
////                    });
////
////                    legend.append("text")
////                    .attr("x", xLocation - 24)
////                    .attr("dy", ".35em")
////                    //                    .style("font-size", "200%")
////                    .style("text-anchor", "end")
////                    .text(function(d) { return d; });
////                }
//
//        //metric legend entry
//        {
//            var legend = rootNode
//                    .append("g")
//                    .data([legendNames[1]])
//                    .attr("class", "legend")
//                    .attr("transform", function (d, i) {
//                        return "translate(" + (legendNames[1].length * 9) + "," + (+yLocation - 20) + ")";
//                    });
//
//            legend.append("rect")
//                    .attr("x", xLocation - 18)
//                    .attr("y", -5)
//                    .attr("width", 10)
//                    .attr("height", 10)
//                    .style("fill", function (d) {
//                        if (d.match(/.*Boundary/)) {
//                            return "D13F31";
//                        } else {
//                            return "72B095";
//                        }
//                    });
//
//            legend.append("text")
//                    .attr("x", xLocation - 24)
//                    .attr("dy", ".35em")
//                    .style("font-size", "160%")
//                    .style("text-anchor", "end")
//                    .text(function (d) {
//                        return d;
//                    });
//        }
//
//        //upper boundary legend entry
//        {
//            var legend = rootNode
//                    .append("g")
//                    .data([legendNames[2]])
//                    .attr("class", "legend")
//                    .attr("transform", function (d, i) {
//                        return "translate(" + 0 + "," + (+yLocation - 20) + ")";
//                    });
//
//            legend.append("rect")
//                    .attr("x", xLocation + width - 18)
//                    .attr("y", -5)
//                    .attr("width", 10)
//                    .attr("height", 10)
//                    .style("fill", function (d) {
//                        if (d.match(/.*Boundar/)) {
//                            return "D13F31";
//                        } else {
//                            return "72B095";
//                        }
//                    });
//
//            legend.append("text")
//                    .attr("x", xLocation + width - 24)
//                    .attr("dy", ".35em")
//                    .style("font-size", "160%")
//                    .style("text-anchor", "end")
//                    .text(function (d) {
//                        return d;
//                    });
//        }
//
//        yLocation = yLocation + height + 50;
//        currentColumn = currentColumn + 1;
//    }

//
//    var legend = rootNode.selectAll(".legend")
//            .data(dimensionNames)
//            .enter().append("g")
//            .attr("class", "legend")
//            .attr("transform", function (d, i) {
//                return "translate(" + (50 + i * width / dimensionNames.length) + "," + (+0) + ")";
//            });
//
//    legend.append("rect")
//            .attr("x", 18)
//            .attr("width", 18)
//            .attr("height", 18)
//            .style("fill", function (d, i) {
//                return "red";
//            });
//
//    legend.append("text")
//            .attr("x", 5)
//            .attr("y", 9)
//            .attr("dy", ".35em")
//            .style("font-size", "140%")
//            .style("text-anchor", "start")
//            .text(function (d) {
//                return d;
//            });

}

var elSpaceColors = ["#72B095", "#D13F31", "#D13F31"];
var elPathwayColors = ["#1C4946", "#1F7872", "#72B095", "#DEDBA7", "#D13F31", "#8C9C9A", "#9DB2B1"];
//       
var lineThickness = new Array("3px", "3px", "3px");
var elSpaceLoaded = false;
var elPathwayLoaded = false;
////
////        function refreshElSpace() {
////
////            elasticitySpaceRequest = null;
////
////            if (window.XMLHttpRequest) {
////                elasticitySpaceRequest = new XMLHttpRequest();
////            } else {
////                elasticitySpaceRequest = new ActiveXObject("Microsoft.XMLHTTP");
////            }
////
////            elasticitySpaceRequest.onreadystatechange = processElasticitySpaceRequestResponse;
////            elasticitySpaceRequest.open("POST", "./REST_WS/" + vars[0] + "/elasticityspace/json", true);
////            elasticitySpaceRequest.setRequestHeader('Content-type', 'application/xml');
////            elasticitySpaceRequest.send("<MonitoredElement id=\"" + vars[1] + "\" level=\"" + vars[2] + "\"/>");
////
////        }
//
//        function processElasticitySpaceRequestResponse() {
//            if (elasticitySpaceRequest.readyState == 4) {
//
//
//                elasticitySpace = JSON.parse(elasticitySpaceRequest.responseText);
//                elSpaceLoaded = true;
//                loadingSpinner.stop();
//                d3.select("#loadingIcon").remove();
//
//
//                drawElSpace(elasticitySpace, vars)
//
//
//                setTimeout(refreshElSpace, 5000);
//            }
//        }



function drawElSpace(elSpace, elSpaceDivID, width, height) {


    if (elSpace && elSpace.dimensions) {

//        var loadingSpinner = document.createElement('div');
        elSpaceDiv = d3.select("#" + elSpaceDivID);
//        elSpaceDiv.setAttribute('id', "loadingIcon");

        drawSpinner(elSpaceDivID);
        elSpaceDiv.selectAll("h2").remove();
        elSpaceDiv.selectAll("svg").remove();
        maxColumns = 1;
        elSpaceSVGHeight = Math.ceil(elSpace.dimensions.length / maxColumns) * (height + 120);
        elSpaceSVG = elSpaceDiv.append("svg")
                .attr("width", width + 500)
                .attr("height", elSpaceSVGHeight)
                .append("g")
                .attr("transform", "translate(" + margin.left + "," + margin.top + ")");
        elSpaceSVG.append("line")
                .attr("x1", -margin.left)
                .attr("y1", -20)
                .attr("x2", window.innerWidth)
                .attr("y2", -20)
                .attr("stroke-width", 1)
                .attr("stroke", "black");
        generateElSpaceChart(elSpace, elSpaceSVG, lineThickness, elSpaceColors, -50, 20, width, height, "Time (s)");
        loadingSpinner.stop();
//        d3.select("#loadingIcon").remove();
    }
}


function drawSpinner(spinnerContainer) {
    var target = document.getElementById(spinnerContainer);
    loadingSpinner.spin(target);
}
