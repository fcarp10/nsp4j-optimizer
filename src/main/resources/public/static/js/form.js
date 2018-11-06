var refreshPeriod = 1000;
var messageIntervalId = setInterval(getMessage, refreshPeriod);

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
            document.getElementById("message").innerText = "Info: framework not running";
        }
    }
    catch (e) {
        return 0;
    }
}

function runOpt(){
var scenario = generateScenario();
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
        }
        return message;
    }
    catch (e) {
        return 0;
    }
}

function generatePaths() {
var scenario = generateScenario();
    try {
            var message = null;
            $.ajax
            ({
                data: scenario,
                url: "paths",
                type: "POST",
                async: false,
                success: function (ans) {
                    message = ans;
                }
            });
            if (message != null) {
               document.getElementById("message").innerText = message;
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

    document.getElementById("countNumberOfUsedServers").checked = true;
    document.getElementById("onePathPerDemand").checked = true;
    document.getElementById("activatePathForService").checked = true;
    document.getElementById("pathsConstrainedByFunctions").checked = true;
    document.getElementById("functionPlacement").checked = true;
    document.getElementById("oneFunctionPerDemand").checked = true;
    document.getElementById("mappingFunctionsWithDemands").checked = true;
    document.getElementById("functionSequenceOrder").checked = true;
    document.getElementById("noParallelPaths").checked = true;
    document.getElementById("initialPlacementAsConstraints").checked = true;
    document.getElementById("synchronizationTraffic").checked = true;

    var model = document.getElementById("model").value;
    if(model === "initial_placement"){
        document.getElementById("initialPlacementAsConstraints").checked = false;
        document.getElementById("synchronizationTraffic").checked = false;
    }
    if(model === "migration"){
            document.getElementById("initialPlacementAsConstraints").checked = false;
            document.getElementById("synchronizationTraffic").checked = false;
    }
    if(model === "replication"){
                document.getElementById("noParallelPaths").checked = false;
    }
    if(model === "migration_replication"){
                document.getElementById("noParallelPaths").checked = false;
                document.getElementById("initialPlacementAsConstraints").checked = false;
    }
    if(model === "all_optimization_models"){
                document.getElementById("noParallelPaths").checked = false;
                document.getElementById("initialPlacementAsConstraints").checked = false;
                document.getElementById("synchronizationTraffic").checked = false;
                document.getElementById("noParallelPaths").disabled = true;
                document.getElementById("initialPlacementAsConstraints").disabled = true;
                document.getElementById("synchronizationTraffic").disabled = true;
    }
    if(model === "migration_replication_rl"){
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

function generateScenario() {

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

    return scenario;
}