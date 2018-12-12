var refreshPeriod = 100;
setInterval(getMessage, refreshPeriod);
var messages = [];

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
        if (message != null && message != "") {
            messages.push(message);
            document.getElementById("message").innerText = "";
            for (var i = 0; i < messages.length; i++)
                document.getElementById("message").innerText += messages[i] +"\n";
            if(messages.length > 2)
               messages.shift();
            if(message == "Info: ready")
                document.getElementById("run_button").removeAttribute("disabled");
            if(message == "Info: topology loaded")
                document.getElementById("run_button").removeAttribute("disabled");
        }
        if(message  == null) {
            document.getElementById("message").innerText = "Info: framework not running";
            document.getElementById("run_button").setAttribute("disabled", "true");
        }
    }
    catch (e) {
        return 0;
    }
}

function loadTopology(){
var scenario = generateScenario();
    try {
        var message = null;
        $.ajax
        ({
            data: scenario,
            url: "load",
            type: "POST",
            async: false,
            success: function (ans) {
                message = ans;
            }
        });
        if (message != null) {
            initializeGraph();
        }
        return message;
    }
    catch (e) {
        return 0;
    }
}

function runOpt(){
var scenario = generateScenario();
document.getElementById("run_button").setAttribute("disabled", "true");
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
    var pathsConstrainedByFunctionsVRC1 = $("#pathsConstrainedByFunctionsVRC1").is(":checked");
    var numberOfActivePathsBoundByService = $("#numberOfActivePathsBoundByService").is(":checked");
    var constraintVRC3 = $("#constraintVRC3").is(":checked");
    var constraintVSC1 = $("#constraintVSC1").is(":checked");

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
            synchronizationTraffic: synchronizationTraffic,
            pathsConstrainedByFunctionsVRC1: pathsConstrainedByFunctionsVRC1,
            numberOfActivePathsBoundByService: numberOfActivePathsBoundByService,
            constraintVRC3: constraintVRC3,
            constraintVSC1: constraintVSC1
        }
    });

    return scenario;
}