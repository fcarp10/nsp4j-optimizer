var refreshPeriod = 1000;
var linkUtilizationGraph;
var serverUtilizationGraph;
var serviceDelayGraph;

var resultsIntervalId = setInterval(getResults, refreshPeriod);

var initialData= [
    { year: '0.1', value: 0 },
    { year: '0.2', value: 0 },
    { year: '0.3', value: 0 },
    { year: '0.4', value: 0 },
    { year: '0.5', value: 0 },
    { year: '0.6', value: 0 },
    { year: '0.7', value: 0 },
    { year: '0.8', value: 0 },
    { year: '0.9', value: 0 },
    { year: '1.0', value: 0 }
];
initializeGraphs(initialData);

function getResults() {
    try {
        var results = null;
        $.ajax
        ({
            url: "results",
            type: "GET",
            async: false,
            success: function (ans) {
                results = ans;
            }
        });
        if (results != null) {
            setSummaryResults(results);
            linkUtilizationGraph.setData(results['luGraph']);
            serverUtilizationGraph.setData(results['xuGraph']);
            serviceDelayGraph.setData(results['sdGraph']);
        } else {
            cleanSummaryResults();
            linkUtilizationGraph.setData(initialData);
            serverUtilizationGraph.setData(initialData);
            serviceDelayGraph.setData(initialData);
        }
    }
    catch (e) {
        return 0;
    }
}

function setSummaryResults(results){
      document.getElementById("avgLu").innerText = results['luSummary'][0];
      document.getElementById("minLu").innerText = results['luSummary'][1];
      document.getElementById("maxLu").innerText = results['luSummary'][2];
      document.getElementById("stdLu").innerText = results['luSummary'][3];
      document.getElementById("avgXu").innerText = results['xuSummary'][0];
      document.getElementById("minXu").innerText = results['xuSummary'][1];
      document.getElementById("maxXu").innerText = results['xuSummary'][2];
      document.getElementById("stdXu").innerText = results['xuSummary'][3];
      document.getElementById("avgF").innerText = results['fuSummary'][0];
      document.getElementById("minF").innerText = results['fuSummary'][1];
      document.getElementById("maxF").innerText = results['fuSummary'][2];
      document.getElementById("stdF").innerText = results['fuSummary'][3];
      document.getElementById("avgSd").innerText = results['sdSummary'][0];
      document.getElementById("minSd").innerText = results['sdSummary'][1];
      document.getElementById("maxSd").innerText = results['sdSummary'][2];
      document.getElementById("stdSd").innerText = results['sdSummary'][3];
      document.getElementById("mgr").innerText = results['migrationsNum'];
      document.getElementById("rep").innerText = results['replicationsNum'];
}

function cleanSummaryResults(){
      var value = 0;
      document.getElementById("avgLu").innerText = value;
      document.getElementById("minLu").innerText = value;
      document.getElementById("maxLu").innerText = value;
      document.getElementById("stdLu").innerText = value;
      document.getElementById("avgXu").innerText = value;
      document.getElementById("minXu").innerText = value;
      document.getElementById("maxXu").innerText = value;
      document.getElementById("stdXu").innerText = value;
      document.getElementById("avgF").innerText = value;
      document.getElementById("minF").innerText = value;
      document.getElementById("maxF").innerText = value;
      document.getElementById("stdF").innerText = value;
      document.getElementById("avgSd").innerText = value;
      document.getElementById("minSd").innerText = value;
      document.getElementById("maxSd").innerText = value;
      document.getElementById("stdSd").innerText = value;
      document.getElementById("mgr").innerText = value;
      document.getElementById("rep").innerText = value;
}

function initializeGraphs(data) {
    linkUtilizationGraph = new Morris.Bar({
      // ID of the element in which to draw the chart.
      element: 'linkUtilization',
      barColors: ['#BC213F'],
      // Chart data records -- each entry in this array corresponds to a point on
      // the chart.
      data,
      // The name of the data record attribute that contains x-values.
      xkey: 'year',
      // A list of names of data record attributes that contain y-values.
      ykeys: ['value'],
      // Labels for the ykeys -- will be displayed when you hover over the
      // chart.
      labels: ['Value']
    });

    serverUtilizationGraph = new Morris.Bar({
      // ID of the element in which to draw the chart.
      element: 'serverUtilization',
      barColors: ['#BC213F'],
      // Chart data records -- each entry in this array corresponds to a point on
      // the chart.
      data,
      // The name of the data record attribute that contains x-values.
      xkey: 'year',
      // A list of names of data record attributes that contain y-values.
      ykeys: ['value'],
      // Labels for the ykeys -- will be displayed when you hover over the
      // chart.
      labels: ['Value']
    });

    serviceDelayGraph = new Morris.Bar({
      // ID of the element in which to draw the chart.
      element: 'serviceDelay',
      barColors: ['#BC213F'],
      // Chart data records -- each entry in this array corresponds to a point on
      // the chart.
      data,
      // The name of the data record attribute that contains x-values.
      xkey: 'year',
      // A list of names of data record attributes that contain y-values.
      ykeys: ['value'],
      // Labels for the ykeys -- will be displayed when you hover over the
      // chart.
      labels: ['Value']
    });
}
