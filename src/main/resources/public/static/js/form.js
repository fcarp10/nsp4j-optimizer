var shortPeriod = 100;
var longPeriod = 3000;
var intervalMessages = setInterval(getMessage, longPeriod);
var connected = false;
var messages = [];
var numMessages = 13;

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
            if(messages.length >= numMessages)
               messages.shift();
            if(message == "Info: ready"){
                document.getElementById("run_button").removeAttribute("disabled");
                document.getElementById("stop_button").setAttribute("disabled", "true");
                longRefresh();
            }
            if(message == "Info: topology loaded"){
                document.getElementById("run_button").removeAttribute("disabled");
                document.getElementById("stop_button").setAttribute("disabled", "true");
                longRefresh();
            }
            if(!connected){
                shortRefresh();
                connected = true;
            }
        }
        if(message  == null) {
            document.getElementById("message").innerText = "Info: framework not running";
            document.getElementById("run_button").setAttribute("disabled", "true");
            document.getElementById("stop_button").setAttribute("disabled", "true");
            clearInterval(intervalMessages);
            if(connected) {
                longRefresh();
                connected = false;
            }
        }
    }
    catch (e) {
        return 0;
    }
}

function shortRefresh() {
   clearInterval(intervalMessages);
   intervalMessages = setInterval(getMessage, shortPeriod);
}

function longRefresh() {
    clearInterval(intervalMessages);
    intervalMessages = setInterval(getMessage, longPeriod);
}

function loadTopology(){
shortRefresh();
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
shortRefresh();
var scenario = generateScenario();
document.getElementById("run_button").setAttribute("disabled", "true");
document.getElementById("stop_button").removeAttribute("disabled");
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

function stopOpt(){
longRefresh();
document.getElementById("stop_button").setAttribute("disabled", "true");
document.getElementById("run_button").removeAttribute("disabled");
    try {
        var message = null;
        $.ajax
        ({
            url: "stop",
            type: "GET",
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
shortRefresh();
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

    // Common constraints
    document.getElementById("rpc1").checked = true;
    document.getElementById("rpc2").checked = true;
    document.getElementById("pfc1").checked = true;
    document.getElementById("pfc2").checked = true;
    document.getElementById("fdc1").checked = true;
    document.getElementById("fdc2").checked = true;
    document.getElementById("fdc3").checked = true;
    document.getElementById("fdc4").checked = true;

    // Model specific constraints
    var model = document.getElementById("model").value;
    if(model === "init"){
        document.getElementById("ipc").checked = true;
        document.getElementById("ipmgrc").checked = true;
        document.getElementById("repc").checked = false;
    }
    if(model === "mgr"){
        document.getElementById("ipc").checked = false;
        document.getElementById("ipmgrc").checked = true;
        document.getElementById("repc").checked = false;
    }
    if(model === "rep"){
        document.getElementById("ipc").checked = false;
        document.getElementById("ipmgrc").checked = false;
        document.getElementById("repc").checked = true;
    }
    if(model === "mgrep"){
        document.getElementById("ipc").checked = false;
        document.getElementById("ipmgrc").checked = false;
        document.getElementById("repc").checked = false;
    }

    document.getElementById("rc").checked = false;
    document.getElementById("fxc").checked = false;
    document.getElementById("sdc").checked = false;
}

function setDecimals(value) {
    value = value.toFixed(1);
}

function generateScenario() {
    // model
    var inputFileName = document.getElementById("inputFileName").value;
    var objectiveFunction = document.getElementById("objectiveFunction").value;
    var maximization = $("#max").is(":checked");
    var weights = parseFloat(document.getElementById("lu").value).toFixed(1) + "-" + parseFloat(document.getElementById("xu").value).toFixed(1) + "-" + parseFloat(document.getElementById("maxU").value).toFixed(1);
    var model = document.getElementById("model").value;
    // general constraints
    var RPC1 = $("#RPC1").is(":checked");
    var RPC2 = $("#RPC2").is(":checked");
    var PFC1 = $("#PFC1").is(":checked");
    var PFC2 = $("#PFC2").is(":checked");
    var FDC1 = $("#FDC1").is(":checked");
    var FDC2 = $("#FDC2").is(":checked");
    var FDC3 = $("#FDC3").is(":checked");
    var FDC4 = $("#FDC4").is(":checked");
    // specific constraints
    var IPC = $("#IPC").is(":checked");
    var IPMGRC = $("#IPMGRC").is(":checked");
    var REPC = $("#REPC").is(":checked");
    // extra constraints
    var RC = $("#RC").is(":checked");
    var FXC = $("#FXC").is(":checked");
    var SDC = $("#SDC").is(":checked");
    var DIC1 = $("#DIC1").is(":checked");
    var DVC1 = $("#DVC1").is(":checked");
    var DVC2 = $("#DVC2").is(":checked");
    var DVC3 = $("#DVC3").is(":checked");

    var scenario = JSON.stringify({
        inputFileName: inputFileName,
        objectiveFunction: objectiveFunction,
        maximization: maximization,
        weights: weights,
        model: model,
        constraints :{
            // general constraints
            RPC1: RPC1,
            RPC2: RPC2,
            PFC1: PFC1,
            PFC2: PFC2,
            FDC1: FDC1,
            FDC2: FDC2,
            FDC3: FDC3,
            FDC4: FDC4,
            // specific constraints
            IPC: IPC,
            IPMGRC: IPMGRC,
            REPC: REPC,
            // extra constraints
            RC: RC,
            FXC: FXC,
            SDC: SDC,
            DIC1: DIC1,
            DVC1: DVC1,
            DVC2: DVC2,
            DVC3: DVC3
        }
    });

    return scenario;
}