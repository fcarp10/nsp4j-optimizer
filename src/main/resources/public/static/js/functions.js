var refreshIntervalId;
function startOpt() {

    var inputFilesName = document.getElementById("inputFilesName").value;
    var objective = document.getElementById("objective").value;
    var useCase = document.getElementById("useCase").value;
    var maximization = $("#max").is(":checked");
    var setLinkUtilizationExpr = $("#setLinkUtilizationExpr").is(":checked");
    var setServerUtilizationExpr = $("#setServerUtilizationExpr").is(":checked");
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
    var reroutingMigration = $("#reroutingMigration").is(":checked");

    var scenario = JSON.stringify({
            inputFilesName: inputFilesName,
            objective: objective,
            useCase: useCase,
            maximization: maximization,
            constraints :{
                setLinkUtilizationExpr: setLinkUtilizationExpr,
                setServerUtilizationExpr: setServerUtilizationExpr,
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
                reroutingMigration: reroutingMigration
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
            document.getElementById("lu").innerText = results['avgLu'] + ' - ' + results['minLu'] + ' - ' + results['maxLu'] + ' - ' + results['vrcLu'];
            document.getElementById("xu").innerText = results['avgXu'] + ' - ' + results['minXu'] + ' - ' + results['maxXu'] + ' - ' + results['vrcXu'];
            document.getElementById("fp").innerText = results['avgFu'] + ' - ' + results['minFu'] + ' - ' + results['maxFu'] + ' - ' + results['vrcFu'];
            document.getElementById("path").innerText = results['avgPathLength'];
            document.getElementById("mgr-rep").innerText = results['numOfMigrations'] + ' - ' + results['numOfReplicas'];
            document.getElementById("cost").innerText = results['cost'];
        } else {
            document.getElementById("lu").innerText = "0.0 - 0.0 - 0.0 - 0.0";
            document.getElementById("xu").innerText = "0.0 - 0.0 - 0.0 - 0.0";
            document.getElementById("fp").innerText = "0.0 - 0.0 - 0.0 - 0.0";
            document.getElementById("path").innerText = "0.0";
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

    document.getElementById("setLinkUtilizationExpr").disabled = false;
    document.getElementById("setServerUtilizationExpr").disabled = false;
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
    document.getElementById("reroutingMigration").disabled = false;

    var useCase = document.getElementById("useCase").value;
    if(useCase === "init"){
        document.getElementById("noParallelPaths").checked = true;
        document.getElementById("initialPlacementAsConstraints").checked = false;
        document.getElementById("reroutingMigration").checked = false;
    }
    if(useCase === "mgr"){
            document.getElementById("noParallelPaths").checked = true;
            document.getElementById("initialPlacementAsConstraints").checked = false;
            document.getElementById("reroutingMigration").checked = true;
    }
    if(useCase === "rep"){
                document.getElementById("noParallelPaths").checked = false;
                document.getElementById("initialPlacementAsConstraints").checked = true;
                document.getElementById("reroutingMigration").checked = false;
    }
    if(useCase === "rep_mgr"){
                document.getElementById("noParallelPaths").checked = false;
                document.getElementById("initialPlacementAsConstraints").checked = false;
                document.getElementById("reroutingMigration").checked = true;
    }
    if(useCase === "all"){
                document.getElementById("noParallelPaths").disabled = true;
                document.getElementById("initialPlacementAsConstraints").disabled = true;
                document.getElementById("reroutingMigration").disabled = true;
    }
    if(useCase === "exp"){
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
                document.getElementById("reroutingMigration").disabled = true;
    }
}