var refreshPeriod = 1000;
var initialData= [
        { year: '0.1', value: 0.1 },
        { year: '0.2', value: 0.2 },
        { year: '0.3', value: 0.3 },
        { year: '0.4', value: 0.4 },
        { year: '0.5', value: 0.5 },
        { year: '0.6', value: 0.6 },
        { year: '0.7', value: 0.7 },
        { year: '0.8', value: 0.8 },
        { year: '0.9', value: 0.9 },
        { year: '1.0', value: 1.0 }
      ];
var linkUtilizationGraph;
var serverUtilizationGraph;
var serviceDelayGraph;
linkUtilizationGraph(initialData);
serverUtilizationGraph(initialData);
serviceDelayGraph(initialData);
var resultsIntervalId = setInterval(getResults, refreshPeriod);

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
        }
    }
    catch (e) {
        return 0;
    }
}

function setSummaryResults(results){
    document.getElementById("luSummary").innerText = results['luSummary'][0] + ' - ' + results['luSummary'][1] + ' - ' + results['luSummary'][2] + ' - ' + results['luSummary'][3];
    document.getElementById("xuSummary").innerText = results['xuSummary'][0] + ' - ' + results['xuSummary'][1] + ' - ' + results['xuSummary'][2] + ' - ' + results['xuSummary'][3];
    document.getElementById("fuSummary").innerText = results['fuSummary'][0] + ' - ' + results['fuSummary'][1] + ' - ' + results['fuSummary'][2] + ' - ' + results['fuSummary'][3];
    document.getElementById("sdSummary").innerText = results['sdSummary'][0] + ' - ' + results['sdSummary'][1] + ' - ' + results['sdSummary'][2] + ' - ' + results['sdSummary'][3];
    document.getElementById("extra").innerText = results['avgPathLength'] + ' - ' + results['totalTraffic'] + ' - ' + results['trafficLinks'];
    document.getElementById("mgr-rep").innerText = results['migrationsNum'] + ' - ' + results['replicationsNum'];
    document.getElementById("cost").innerText = results['cost'];
}

function cleanSummaryResults(){
    document.getElementById("luSummary").innerText = "0.0 - 0.0 - 0.0 - 0.0";
    document.getElementById("xuSummary").innerText = "0.0 - 0.0 - 0.0 - 0.0";
    document.getElementById("fuSummary").innerText = "0.0 - 0.0 - 0.0 - 0.0";
    document.getElementById("extra").innerText = "0.0 - 0.0 - 0.0";
    document.getElementById("mgr-rep").innerText = "0 - 0";
    document.getElementById("cost").innerText = "0.0";
}

function linkUtilizationGraph(data) {
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
}

function serverUtilizationGraph(data) {
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
}

function serviceDelayGraph(data) {
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