
var jsMsChart = {};

jsMsChart.svgns = 'http://www.w3.org/2000/svg';

jsMsChart.drawLine = function(xs, ys, xe, ye, color, lineWidth) {
	var line = document.createElementNS(jsMsChart.svgns, 'line');
	line.setAttributeNS(null, 'x1', xs);
	line.setAttributeNS(null, 'y1', ys);
	line.setAttributeNS(null, 'x2', xe);
	line.setAttributeNS(null, 'y2', ye);
	line.setAttributeNS(null, 'style', 'stroke: ' + color + '; stroke-width: ' + lineWidth + ';' );

	return line;
};

jsMsChart.drawText = function(xs, ys, color, caption, alignment, anchor) {
	var text = document.createElementNS(jsMsChart.svgns, 'text');
    text.setAttributeNS(null, 'x', xs);
    text.setAttributeNS(null, 'y', ys);
	text.setAttributeNS(null, 'fill', color);

	text.setAttributeNS(null, 'alignment-baseline', alignment);
	text.setAttributeNS(null, 'text-anchor', anchor);

	text.innerHTML = caption;

	return text;
};

jsMsChart.measureSVGElementSize = function(svgElement) {

	var svg = document.createElementNS(jsMsChart.svgns, 'svg');

    document.body.appendChild(svg);
    svg.append(svgElement);

    var size = svgElement.getBBox();

    svg.removeChild(svgElement);
    document.body.removeChild(svg);

    return size;
};

jsMsChart.randomColor = function() {
	var chars = '0123456789ABCDEF'.split('');
	var color = '#';

	for (var i = 0; i < 6; i++)
		color += chars[Math.floor(Math.random() * 16)];
	return color;
};

jsMsChart.Tooltip = function(elementId) {

	var color = 'orange';
	var opacity = '.5';

	var tipSize = 5;

	var x = 0;
	var y = 0;

	var width = 0;
	var height = 0;

	var padding = 5;

	var box = createBox();
	var text = createText();

	var element = createElement();

	function createBox() {
		var box = document.createElementNS(jsMsChart.svgns, 'polygon');
		box.setAttributeNS(null, 'style', 'fill: ' + color + '; opacity: ' + opacity + ';' );

		return box;
	}

	function createText() {
		var text = document.createElementNS(jsMsChart.svgns, 'text');
		text.setAttributeNS(null, 'x', width / 2);
		text.setAttributeNS(null, 'y', height / 2);
		text.setAttributeNS(null, 'alignment-baseline', 'middle');
		text.setAttributeNS(null, 'text-anchor', 'middle');
		return text;
	}

	function createElement() {
		var element =  document.createElementNS(jsMsChart.svgns, 'g');
        element.setAttributeNS(null, 'id', elementId);
		element.append(box);
		element.append(text);
		return element;
	}

	function resize() {

	    width = text.getBBox().width + 2 * padding;
		height = text.getBBox().height + 2 * padding;

		box.setAttributeNS(null, 'points', '0,0 ' + width + ',0 ' +
            width + ',' + height + ' ' + (width / 2 + tipSize) + ',' + height + ' ' +
            width / 2 + ',' + (height + tipSize) + ' ' + (width / 2 - tipSize) + ',' + height + ' ' +
            (width / 2 - tipSize) + ',' + height + ' 0,' + height);

		text.setAttributeNS(null, 'x', width / 2);
		text.setAttributeNS(null, 'y', height / 2);

		moveGroup(x, y);
	}

	function moveGroup(x, y) {
		element.setAttributeNS(null, 'transform', 'translate(' + (x - width / 2) + ' ' + (y - height - tipSize) +')');
	}

	this.setPosition = function(valX, valY) {
		x = valX;
		y = valY;
		moveGroup(x, y);
	};

	this.setText = function(caption) {
		text.innerHTML = caption;
		resize();
	};

	this.getElement = function() {
		this.hide();
		return element;
	};

	this.show = function() {
		box.style.visibility = 'visible';
		text.style.visibility = 'visible';
	};

	this.hide = function() {
		box.style.visibility = 'hidden';
		text.style.visibility = 'hidden';
	};

    this.setOpacity = function(value) {
        opacity = value;
        box.setAttributeNS(null, 'style', 'fill: ' + color + '; opacity: ' + opacity + ';' );
    };

    this.setColor = function(value) {
        color = value;
        box.setAttributeNS(null, 'style', 'fill: ' + color + '; opacity: ' + opacity + ';' );
    };

	this.setTextColor = function(value) {
        text.setAttributeNS(null, 'style', 'fill: ' + value + ';');
    };
};

jsMsChart.Axis = function(width, height, axisColor, axisOffsetX, axisOffsetY, domain) {

	var ticksPerPixel = 1/40;
	var tickLength = 8;

	var axisGroupX;
	var axisGroupY;

	var validAxisFractions = [1, 1/2, 1/4, 3/4, 1/5, 2/5, 3/5, 4/5, 1/8, 1/10, 3/10, 7/10, 9/10, 1/20, 1/40, 3/40, 1/50, 1/100];

	var startX = axisOffsetX;
	var endX = width;
	var startY = axisOffsetY / 2;
	var endY = height - axisOffsetY;

	var canvasWidth = endX - startX;
	var canvasHeight = endY - startY;

	var minX = (domain.x.min * .9) | 0;
	var maxX = (domain.x.max * 1.1) | 0;
	var minY = (domain.y.min * 1) | 0;
	var maxY = (domain.y.max * 1.2) | 0;

	var rangeX = (maxX - minX);
	var rangeY = (maxY - minY);

	var ratioX = canvasWidth / rangeX;
	var ratioY = canvasHeight / rangeY;

	var offsetX  = startX - minX * ratioX;

	var amountAxisMarkersX = (canvasWidth * ticksPerPixel) | 0;
	var amountAxisMarkersY = (canvasHeight * ticksPerPixel) | 0;

    amountAxisMarkersX = amountAxisMarkersX < 1 ? 1 : amountAxisMarkersX;
    amountAxisMarkersY = amountAxisMarkersY < 1 ? 1 : amountAxisMarkersY;

	var realAxisFractionX = rangeX / amountAxisMarkersX;
	var realAxisFractionY = rangeY / amountAxisMarkersY;

	var fractionX = realAxisFractionX / Math.pow(10, (Math.log10(rangeX) + 1) | 0);
	var fractionY = realAxisFractionY / Math.pow(10, (Math.log10(rangeY) + 1) | 0);

	var matchedAxisFractionX = matchFraction(fractionX, validAxisFractions);
	var matchedAxisFractionY = matchFraction(fractionY, validAxisFractions);

	var axisCaptionX = jsMsChart.drawText(endX / 2, 0, axisColor, '', 'hanging', 'middle');
	var axisCaptionY = jsMsChart.drawText(0, endY / 2, axisColor, '', 'middle', 'end');

	function matchFraction(targetFraction, availableFractions) {

		var minDiff = Infinity;
		var matchedFraction = null;

		for (var index in availableFractions) {
			var fraction = availableFractions[index];
			var diff = Math.abs(targetFraction - fraction);
			if (diff < minDiff) {
				minDiff = diff;
				matchedFraction = fraction;
			}
		}

		return matchedFraction;
	}

	function nextPowerOfTen(value) {
		return Math.pow(10, (Math.log10(value) + 1) | 0);
	}

	function drawAxisX(axisGroup, matchedAxisFractionX) {

		var axisGroupX = document.createElementNS(jsMsChart.svgns, 'g');
		axisGroupX.setAttributeNS(null, 'id', 'axisX');

		axisGroupX.append(jsMsChart.drawLine(startX, startY, startX, endY, axisColor, 1));

		var markerPosition = 0;
		var markerCounter = 0;

		while (markerPosition < maxX) {
			markerPosition = markerCounter * matchedAxisFractionX * nextPowerOfTen(rangeX);
			if (markerPosition > minX && markerPosition < maxX) {

				var x = markerPosition * ratioX;
				var y = endY;

				axisGroupX.append(jsMsChart.drawLine(x + offsetX, y, x + offsetX, y + tickLength, axisColor, 1));
				axisGroupX.append(jsMsChart.drawText(x + offsetX, y + tickLength, axisColor, displayNumericValue(markerPosition), 'hanging', 'middle'));
			}
			markerCounter++;
		}

		return axisGroupX;
	}

	function drawAxisY(axisGroup, matchedAxisFractionY) {

		var axisGroupY = document.createElementNS(jsMsChart.svgns, 'g');
		axisGroupY.setAttributeNS(null, 'id', 'axisY');

		axisGroupY.append(jsMsChart.drawLine(startX, endY, endX, endY, axisColor, 1));

		var markerPosition = 0;
		var markerCounter = 0;

		while (markerPosition < rangeY) {
			markerPosition = markerCounter * matchedAxisFractionY * nextPowerOfTen(rangeY);
			if (markerPosition < rangeY) {

				var x = startX;
				var y = endY - markerPosition * ratioY;

				axisGroupY.append(jsMsChart.drawLine(x, y, x - tickLength, y, axisColor, 1));
				axisGroupY.append(jsMsChart.drawText(x - tickLength, y, axisColor, displayNumericValue(markerPosition), 'middle', 'end'));
			}
			markerCounter++;
		}

		return axisGroupY;
	}

	function displayNumericValue(value) {

		value = value > 0 ? Math.round(value) : value;

		if (value >= 1000000000 && value < 9999999999) {
			return (value / 1000000) + 'G';
		} else if (value >= 1000000){
			return (value / 1000000) + 'M';
		} else if (value >= 1000) {
			return (value / 1000) + 'K';
		}

		return value;
	}

	this.getOffsetX = function() {
		return offsetX;
	};

	this.getEndY = function() {
		return endY;
	};

	this.getRatioX = function() {
		return ratioX;
	};

	this.getRatioY = function() {
		return ratioY;
	};

	this.getElement = function() {
		var axisGroup = document.createElementNS(jsMsChart.svgns, 'g');
		axisGroup.setAttributeNS(null, 'id', 'axisGroup');

        axisGroupX = drawAxisX(axisGroup, matchedAxisFractionX);
		axisGroup.append(axisGroupX);
		axisGroup.append(axisCaptionX);

        axisGroupY = drawAxisY(axisGroup, matchedAxisFractionY);
		axisGroup.append(axisGroupY);
		axisGroup.append(axisCaptionY);

		return axisGroup;
	};

	this.setAxisCaptions = function(captionX, captionY) {

        axisCaptionX.innerHTML = captionX;

        var textHeight = axisCaptionX.getBBox().height / 2;
        var axisHeightX = axisGroupX.getBBox().height + axisGroupX.getBBox().y;

        axisCaptionX.setAttributeNS(null, 'y', axisHeightX + textHeight);

		axisCaptionY.innerHTML = captionY;

		var axisYPosX = (axisCaptionY.getBBox().height * 1.1) / 2;
		axisCaptionY.setAttributeNS(null, 'x', axisYPosX);
		axisCaptionY.setAttributeNS(null, 'transform', 'rotate(-90, ' + axisYPosX + ', ' + (endY / 2) + ')');
	}
};

jsMsChart.DataPoint = function (startX, startY, endX, endY, color, hoverColor, data, barStrokeWidth) {

	var line;

    this.getId = function() {
        return data.id;
    };

	this.getElement = function() {
        var group = document.createElementNS(jsMsChart.svgns, 'g');
        line = jsMsChart.drawLine(startX, startY, endX, endY, color, barStrokeWidth);
        group.setAttributeNS(null, 'id', data.id);
        group.append(line);

        return group;
	};

	this.setColor = function(color) {
	    if (line) {
            line.setAttributeNS(null, 'style', 'stroke: ' + color + '; stroke-width: ' + barStrokeWidth + '; cursor: pointer;');
        }
    };

	this.resetColor = function() {
        if (line) {
            line.setAttributeNS(null, 'style', 'stroke: ' + color + '; stroke-width: ' + barStrokeWidth + '; cursor: pointer;');
        }
    }

};

jsMsChart.Chart = function (element) {

    var self = this;
    var data;
    var config;
    var selected;

    var selectionColor = 'red';
	var hoverColor = '#f4bc42';
	var textHoverColor = 'black';
    var hoverOpacity = .5;

	var digits = 3;

	var axisColor = 'darkgray';

	var axisOffsetX = 65;
	var axisOffsetY = 65;

	var axisCaptionX = '';
	var axisCaptionY = '';

	var barStrokeWidth = 5;

	var domain;

	var axis;
	var tooltip;
	var selectionTooltip;

	var svg = document.createElementNS(jsMsChart.svgns, 'svg');
	svg.setAttributeNS(null,'id','svgChart');
	svg.setAttributeNS(null,'height','100%');
	svg.setAttributeNS(null,'width','100%');
	element.appendChild(svg);

	var width = svg.getBoundingClientRect().width;
	var height = svg.getBoundingClientRect().height;

	this.redraw = function() {
		width = svg.getBoundingClientRect().width;
		height = svg.getBoundingClientRect().height;
		self.setData(self.getData());
		if (selected) {
			self.select(selected.getId());
		}
	};

	function initialiseAxis() {
		axis = new jsMsChart.Axis(width, height, axisColor, axisOffsetX, axisOffsetY, domain);
		svg.append(axis.getElement());
		axis.setAxisCaptions(axisCaptionX, axisCaptionY)
	}

    function determineDomain(dataArray) {

	    domain = {x: {min: Infinity, max: -Infinity}, y: {min: Infinity, max: -Infinity}};

        for (var index in Object.keys(dataArray)) {

        	var seriesKey = Object.keys(dataArray)[index];

            var points = dataArray[seriesKey].points;

            for (var pointIndex in points) {

            	var point = points[pointIndex];

            	if (point.x < domain.x.min) {
                    domain.x.min = point.x;
                }
                if (point.x > domain.x.max) {
                    domain.x.max = point.x;
                }
                if (point.y < domain.y.min) {
                    domain.y.min = point.y;
                }
                if (point.y > domain.y.max) {
                    domain.y.max = point.y;
                }
            }

            // Start y scale from 0
            domain.y.min = 0;

            domain.x.range = domain.x.max - domain.x.min;
            domain.y.range = domain.y.max - domain.y.min;
        }
    }

    function createDataGroup(dataArray) {

        var dataGroup =  document.createElementNS(jsMsChart.svgns, 'g');
        dataGroup.setAttributeNS(null, 'id', 'dataGroup');

        var offsetY = 0;

        for (var index in Object.keys(dataArray)) {

        	var seriesKey = Object.keys(dataArray)[index];

            var series = dataArray[seriesKey];

            var name = series.name;
            var color = series.color !== '' ? series.color : jsMsChart.randomColor();
            var dataPoints = series.points;

            var seriesGroup = document.createElementNS(jsMsChart.svgns, 'g');
            seriesGroup.setAttributeNS(null, 'id', 'series[' + seriesKey + ']');

            for (var point in dataPoints) {
                seriesGroup.append(drawData(color, dataPoints[point]));
            }

            var legendGroup = document.createElementNS(jsMsChart.svgns, 'g');
            var legendCaption = jsMsChart.drawText(width, offsetY, color, name, 'hanging', 'end');
            offsetY += jsMsChart.measureSVGElementSize(legendCaption).height * 1.1;

            legendGroup.append(legendCaption);
            seriesGroup.append(legendGroup);

            dataGroup.append(seriesGroup);
        }

        return dataGroup;
    }

    function createTooltip(elementId, color, textColor) {
        var tooltip = new jsMsChart.Tooltip(elementId);
        tooltip.setOpacity(hoverOpacity);
        tooltip.setColor(color);
        tooltip.setTextColor(textColor);

        return tooltip;
    }

    this.getData = function() {
	    return data;
    };

    this.setData = function(dataArray) {

	    data = dataArray;

	    while (svg.firstChild) {
            svg.removeChild(svg.firstChild);
        }

        determineDomain(dataArray);
        initialiseAxis();

        svg.append(createDataGroup(dataArray));

        selectionTooltip = createTooltip('selectionTooltip', selectionColor, 'white');
        svg.append(selectionTooltip.getElement());

        tooltip = createTooltip('hoverTooltip', hoverColor, textHoverColor);
        svg.append(tooltip.getElement());
    };

    this.getSelected = function() {
        return selected;
    };

    this.select = function(value) {

        var lineGroup = svg.getElementById(value);

        if (lineGroup) {
            lineGroup.select();
        }
    };

	function drawData(color, data) {

		var x = data.x * axis.getRatioX();
		var y = data.y * axis.getRatioY();

        var dataPoint = new jsMsChart.DataPoint(x + axis.getOffsetX(), axis.getEndY(), x + axis.getOffsetX(), axis.getEndY() - y, color, selectionColor, data, barStrokeWidth);
        var dataPointElement = dataPoint.getElement();

        dataPointElement.addEventListener('mouseenter', function( event ) {

		    if (selected !== dataPoint) {
                dataPoint.setColor(hoverColor);
                tooltip.setPosition(x + axis.getOffsetX(), axis.getEndY() - y);
                tooltip.setText(data.x.toFixed(digits) + ', ' + data.y.toFixed(digits));
                tooltip.show();
            }
		});

        function changeSelection() {
                if (selected) {
                    selected.resetColor(false);
                }

                dataPoint.setColor(selectionColor);
                selected = dataPoint;

                selectionTooltip.setPosition(x + axis.getOffsetX(), axis.getEndY() - y);
                selectionTooltip.setText(data.x.toFixed(digits) + ', ' + data.y.toFixed(digits));

                tooltip.hide();
                selectionTooltip.show();
                return true;
        }

        dataPointElement.select = changeSelection;

        dataPointElement.addEventListener('click', function( event ) {

            if (selected === dataPoint) {
                selected.resetColor();
                selectionTooltip.hide();
                selected = null;
            } else {
                changeSelection();
                self.click(selected.getId());
            }
        });

        dataPointElement.addEventListener('mouseleave', function( event ) {

            if (selected !== dataPoint) {
                dataPoint.resetColor();
            }

		    tooltip.hide();
		});

		return dataPointElement;
	}

    this.getConfig = function() {
        return config;
    };

    this.setConfig = function(value) {

        config = value;

        self.setAxisCaptions(config.axisCaptionX, config.axisCaptionY);
        self.setFloatingDigits(config.floatingDigits);
        self.setBarStrokeWidth(config.barStrokeWidth);
        self.setSelectionColor(config.selectionColor);
        self.setHoverOpacity(config.hoverOpacity);
        self.setHoverColor(config.hoverColor);
        self.setTextHoverColor(config.textHoverColor);
        self.setAxisColor(config.axisCOlor);
        self.setAxisOffsetX(config.axisOffsetX);
        self.setAxisOffsetY(config.axisOffsetY);
    };

	this.setFloatingDigits = function(value) {
        if (value) {
            digits = value;
        }
	};

    this.setSelectionColor = function(value) {
        if (value) {
            selectionColor = value;
        }
    };

    this.setHoverOpacity = function(value) {
        if (value) {
            hoverOpacity = value;
        }
    };

	this.setHoverColor = function(value) {
        if (value) {
            hoverColor = value;
        }
	};

    this.setTextHoverColor = function(value) {
        if (value) {
            textHoverColor = value;
        }
    };

    this.setBarStrokeWidth = function(value) {
	    if (value) {
            barStrokeWidth = value;
        }
    };

    this.setAxisColor = function(value) {
        if (value) {
            axisColor = value;
        }
    };

	this.setAxisCaptions = function(valX, valY) {

        axisCaptionX = valX;
        axisCaptionY = valY;

		if (axis) {
            axis.setAxisCaptions(valX, valY);
        }
	};

	this.setAxisOffsetX = function(value) {
	    if (value) {
            axisOffsetX = value;
        }
    };

    this.setAxisOffsetY = function(value) {
        if (value) {
            axisOffsetY = value;
        }
    };

	this.getValue = function() {
	    return value;
    }
};


// --- Added for Vaadin Flow (SP-09/SP-13): the chart code above is unchanged from the Vaadin 8 version ---
// Called from MsChart.java. Creates the chart once per element, then updates config and data.
window.fiMsChart = function(element, configJson, dataJson) {
    if (!element.$msChart) {
        element.$msChart = new jsMsChart.Chart(element);
        element.$msChart.click = function(id) { element.$server.onValueClick(id); };
        new ResizeObserver(function() { element.$msChart.redraw(); }).observe(element);
    }
    element.$msChart.setConfig(JSON.parse(configJson));
    element.$msChart.setData(JSON.parse(dataJson));
};

// Called from MsChart.java: selects the bar of a point (null: no selection).
window.fiMsChartSelect = function(element, id) {
    if (element.$msChart && id) {
        element.$msChart.select(id);
    }
};
