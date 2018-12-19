var refreshPeriod = 500;
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
        document.getElementById("noParallelPaths").checked = true;
        document.getElementById("initialPlacementAsConstraints").checked = false;
        document.getElementById("synchronizationTraffic").checked = false;
    }
    if(model === "migration"){
            document.getElementById("noParallelPaths").checked = true;
            document.getElementById("initialPlacementAsConstraints").checked = false;
            document.getElementById("synchronizationTraffic").checked = false;
    }
    if(model === "replication"){
                document.getElementById("noParallelPaths").checked = false;
                document.getElementById("initialPlacementAsConstraints").checked = true;
                document.getElementById("synchronizationTraffic").checked = false;
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
    var VAI3 = $("#countNumberOfUsedServers").is(":checked");
    var RPC1 = $("#onePathPerDemand").is(":checked");
    var RPI1 = $("#onePathPerDemand").is(":checked");
    var VRC2 = $("#pathsConstrainedByFunctions").is(":checked");
    var VAC1 = $("#functionPlacement").is(":checked");
    var VAC2 = $("#oneFunctionPerDemand").is(":checked");
    var VAI1 = $("#mappingFunctionsWithDemands").is(":checked");
    var VAC3 = $("#functionSequenceOrder").is(":checked");
    var RPC3 = $("#noParallelPaths").is(":checked");
    var IPC1 = $("#initialPlacementAsConstraints").is(":checked");
    var synchronizationTraffic = $("#synchronizationTraffic").is(":checked");
    var VRC1 = $("#pathsConstrainedByFunctionsVRC1").is(":checked");
    var RPC2 = $("#numberOfActivePathsBoundByService").is(":checked");
    var VRC3 = $("#constraintVRC3").is(":checked");
    var VAI2 = $("#constraintVAI2").is(":checked");
    var VSC1 = $("#constraintVSC1").is(":checked");
    var VSC2 = $("#constraintVSC2").is(":checked");
    var VSC3 = $("#constraintVSC3").is(":checked");
    var DIC1 = $("#constraintDIC1").is(":checked");

    var scenario = JSON.stringify({
        inputFileName: inputFileName,
        objectiveFunction: objectiveFunction,
        maximization: maximization,
        model: model,
        constraints :{
            RPC1: RPC1,
            RPI1: RPI1,
            VAI1: VAI1,
            VAI2: VAI2,
            VAI3: VAI3,
            VAC1: VAC1,
            VAC2: VAC2,
            VAC3: VAC3,
            RPC2: RPC2,
            RPC3: RPC3,
            VRC1: VRC1,
            VRC2: VRC2,
            VRC3: VRC3,
            VSC1: VSC1,
            VSC2: VSC2,
            VSC3: VSC3,
            DIC1: DIC1,
            IPC1: IPC1,
            synchronizationTraffic: synchronizationTraffic
        }
    });

    return scenario;
}