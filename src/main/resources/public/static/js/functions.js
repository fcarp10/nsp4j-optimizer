var refreshIntervalId;
function startOpt() {

    var inputFileName = document.getElementById("inputFileName").value;
    var objectiveFunction = document.getElementById("objectiveFunction").value;
    var maximization = $("#max").is(":checked");
    var model = document.getElementById("model").value;
    var countNumberOfUsedServers = $("#countNumberOfUsedServers").is(":checked");
    var onePathPerDemand = $("#onePathPerDemand").is(":checked");
    var activatePathForService = $("#onePathPerDemand").is(":checked");
    var pathsConstrainedByFunctions = $("#pathsConstrainedByFunctions").is(":checked");
    var functionPlacement = $("#functionPlacement").is(":checked");
    var oneFunctionPerDemand = $("#oneFunctionPerDemand").is(":checked");
    var mappingFunctionsWithDemands = $("#mappingFunctionsWithDemands").is(":checked");
    var functionSequenceOrder = $("#functionSequenceOrder").is(":checked");
    var noParallelPaths = $("#noParallelPaths").is(":checked");
    var initialPlacementAsConstraints = $("#initialPlacementAsConstraints").is(":checked");
    var synchronizationTraffic = $("#synchronizationTraffic").is(":checked");

    var scenario = JSON.stringify({
            inputFileName: inputFileName,
            objectiveFunction: objectiveFunction,
            maximization: maximization,
            model: model,
            constraints :{
                countNumberOfUsedServers: countNumberOfUsedServers,
                onePathPerDemand: onePathPerDemand,
                activatePathForService: activatePathForService,
                pathsConstrainedByFunctions: pathsConstrainedByFunctions,
                functionPlacement: functionPlacement,
                oneFunctionPerDemand: oneFunctionPerDemand,
                mappingFunctionsWithDemands: mappingFunctionsWithDemands,
                functionSequenceOrder: functionSequenceOrder,
                noParallelPaths: noParallelPaths,
                initialPlacementAsConstraints: initialPlacementAsConstraints,
                synchronizationTraffic: synchronizationTraffic
            }
        });

    try {
        var message = null;
        $.ajax
        ({
            data: scenario,
            url: "run",
            type: "POST",
            async: false,
            success: function (ans) {
                message = ans;
            }
        });
        if (message != null) {
            document.getElementById("message").innerText = message;
            setInterval(getResults, 2000);
            refreshIntervalId = setInterval(getMessage, 2000);
        }
        return message;
    }
    catch (e) {
        return 0;
    }
}

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
            document.getElementById("luSummary").innerText = results['luSummary'][0] + ' - ' + results['luSummary'][1] + ' - ' + results['luSummary'][2] + ' - ' + results['luSummary'][3];
            document.getElementById("xuSummary").innerText = results['xuSummary'][0] + ' - ' + results['xuSummary'][1] + ' - ' + results['xuSummary'][2] + ' - ' + results['xuSummary'][3];
            document.getElementById("fuSummary").innerText = results['fuSummary'][0] + ' - ' + results['fuSummary'][1] + ' - ' + results['fuSummary'][2] + ' - ' + results['fuSummary'][3];
            document.getElementById("sdSummary").innerText = results['sdSummary'][0] + ' - ' + results['sdSummary'][1] + ' - ' + results['sdSummary'][2] + ' - ' + results['sdSummary'][3];
            document.getElementById("extra").innerText = results['avgPathLength'] + ' - ' + results['totalTraffic'] + ' - ' + results['trafficLinks'];
            document.getElementById("mgr-rep").innerText = results['migrationsNum'] + ' - ' + results['replicationsNum'];
            document.getElementById("cost").innerText = results['cost'];
        } else {
            document.getElementById("luSummary").innerText = "0.0 - 0.0 - 0.0 - 0.0";
            document.getElementById("xuSummary").innerText = "0.0 - 0.0 - 0.0 - 0.0";
            document.getElementById("fuSummary").innerText = "0.0 - 0.0 - 0.0 - 0.0";
            document.getElementById("extra").innerText = "0.0 - 0.0 - 0.0";
            document.getElementById("mgr-rep").innerText = "0 - 0";
            document.getElementById("cost").innerText = "0.0";
        }
    }
    catch (e) {
        return 0;
    }
}

function getMessage() {
    try {
        var message = null;
        $.ajax
        ({
            url: "message",
            type: "GET",
            async: false,
            success: function (ans) {
                message = ans;
            }
        });
        if (message != null) {
            document.getElementById("message").innerText = message;
        } else {
            document.getElementById("message").innerText = "The server is not running";
            clearInterval(refreshIntervalId);
        }
    }
    catch (e) {
        return 0;
    }
}

function check(elem) {
    document.getElementById("countNumberOfUsedServers").disabled = false;
    document.getElementById("onePathPerDemand").disabled = false;
    document.getElementById("activatePathForService").disabled = false;
    document.getElementById("pathsConstrainedByFunctions").disabled = false;
    document.getElementById("functionPlacement").disabled = false;
    document.getElementById("oneFunctionPerDemand").disabled = false;
    document.getElementById("mappingFunctionsWithDemands").disabled = false;
    document.getElementById("functionSequenceOrder").disabled = false;
    document.getElementById("noParallelPaths").disabled = false;
    document.getElementById("initialPlacementAsConstraints").disabled = false;
    document.getElementById("synchronizationTraffic").disabled = false;

    var useCase = document.getElementById("useCase").value;
    if(useCase === "init"){
        document.getElementById("noParallelPaths").checked = true;
        document.getElementById("initialPlacementAsConstraints").checked = false;
        document.getElementById("synchronizationTraffic").checked = false;
    }
    if(useCase === "mgr"){
            document.getElementById("noParallelPaths").checked = true;
            document.getElementById("initialPlacementAsConstraints").checked = false;
            document.getElementById("synchronizationTraffic").checked = false;
    }
    if(useCase === "rep"){
                document.getElementById("noParallelPaths").checked = false;
                document.getElementById("initialPlacementAsConstraints").checked = true;
                document.getElementById("synchronizationTraffic").checked = true;
    }
    if(useCase === "rep_mgr"){
                document.getElementById("noParallelPaths").checked = false;
                document.getElementById("initialPlacementAsConstraints").checked = false;
                document.getElementById("synchronizationTraffic").checked = true;
    }
    if(useCase === "all"){
                document.getElementById("noParallelPaths").checked = false;
                document.getElementById("initialPlacementAsConstraints").checked = false;
                document.getElementById("synchronizationTraffic").checked = false;
                document.getElementById("noParallelPaths").disabled = true;
                document.getElementById("initialPlacementAsConstraints").disabled = true;
                document.getElementById("synchronizationTraffic").disabled = true;
    }
    if(useCase === "exp"){
                document.getElementById("countNumberOfUsedServers").checked = false;
                document.getElementById("onePathPerDemand").checked = false;
                document.getElementById("activatePathForService").checked = false;
                document.getElementById("pathsConstrainedByFunctions").checked = false;
                document.getElementById("functionPlacement").checked = false;
                document.getElementById("oneFunctionPerDemand").checked = false;
                document.getElementById("mappingFunctionsWithDemands").checked = false;
                document.getElementById("functionSequenceOrder").checked = false;
                document.getElementById("noParallelPaths").checked = false;
                document.getElementById("initialPlacementAsConstraints").checked = false;
                document.getElementById("synchronizationTraffic").checked = false;
                document.getElementById("setLinkUtilizationExpr").disabled = true;
                document.getElementById("setServerUtilizationExpr").disabled = true;
                document.getElementById("countNumberOfUsedServers").disabled = true;
                document.getElementById("onePathPerDemand").disabled = true;
                document.getElementById("activatePathForService").disabled = true;
                document.getElementById("pathsConstrainedByFunctions").disabled = true;
                document.getElementById("functionPlacement").disabled = true;
                document.getElementById("oneFunctionPerDemand").disabled = true;
                document.getElementById("mappingFunctionsWithDemands").disabled = true;
                document.getElementById("functionSequenceOrder").disabled = true;
                document.getElementById("noParallelPaths").disabled = true;
                document.getElementById("initialPlacementAsConstraints").disabled = true;
                document.getElementById("synchronizationTraffic").disabled = true;
    }
}