var shortPeriod = 100;
var longPeriod = 3000;
var intervalMessages = setInterval(getMessage, longPeriod);
var messages = [];
var numMessages = 10;

function getMessage() {
    try {
        var message = null;
        var error = false;
        $.ajax
            ({
                url: "message",
                type: "GET",
                async: false,
                success: function (ans) { successConnection(ans); },
                error: function () { errorConnection(); }
            });
    }
    catch (e) {
        return 0;
    }
}

function errorConnection() {
    if (document.getElementById("message").innerText != "Info: framework not running") {
        document.getElementById("message").innerText = "Info: framework not running";
        document.getElementById("run_button").setAttribute("disabled", "true");
        document.getElementById("stop_button").setAttribute("disabled", "true");
        clearInterval(intervalMessages);
        longRefresh();
    }
}

function successConnection(message) {
    if (message != "") {
        messages.push(message + "\n");
        document.getElementById("message").innerText = messages.join("");
        if (messages.length >= numMessages)
            messages.shift();
        if (message == "Info: ready" || message == "Info: topology loaded") {
            document.getElementById("run_button").removeAttribute("disabled");
            document.getElementById("stop_button").setAttribute("disabled", "true");
            longRefresh();
        }
        if (message == "Info: ready"){
            getResults();
        }
        else {
            shortRefresh();
        }
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

function loadTopology() {
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
            cleanResults();
        }
        return message;
    }
    catch (e) {
        return 0;
    }
}

function runOpt() {
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

function stopOpt() {
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
    var model = document.getElementById("model").value;
    if (model === "init") {
        document.getElementById("IP").checked = true;
        document.getElementById("IP_MGR").checked = true;
        document.getElementById("REP").checked = false;
        document.getElementById("objectiveFunction").getElementsByTagName('option')[1].selected = "selected";
    }
    else if (model === "mgr") {
        document.getElementById("IP").checked = false;
        document.getElementById("IP_MGR").checked = true;
        document.getElementById("REP").checked = false;
        document.getElementById("objectiveFunction").getElementsByTagName('option')[2].selected = "selected";
    }
    else if (model === "rep") {
        document.getElementById("IP").checked = false;
        document.getElementById("IP_MGR").checked = false;
        document.getElementById("REP").checked = true;
        document.getElementById("objectiveFunction").getElementsByTagName('option')[2].selected = "selected";;
    }
    else if (model === "mgrep") {
        document.getElementById("IP").checked = false;
        document.getElementById("IP_MGR").checked = false;
        document.getElementById("REP").checked = false;
        document.getElementById("objectiveFunction").getElementsByTagName('option')[2].selected = "selected";
    }
    else if (model === "mgrep_rl") {
        document.getElementById("objectiveFunction").getElementsByTagName('option')[2].selected = "selected";
    }
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
    var RP1 = $("#RP1").is(":checked");
    var RP2 = $("#RP2").is(":checked");
    var PF1 = $("#PF1").is(":checked");
    var PF2 = $("#PF2").is(":checked");
    var FD1 = $("#FD1").is(":checked");
    var FD2 = $("#FD2").is(":checked");
    var FD3 = $("#FD3").is(":checked");
    var FD4 = $("#FD4").is(":checked");
    // additional constraints
    var ST = $("#ST").is(":checked");
    var SD = $("#SD").is(":checked");
    // specific constraints
    var IP = $("#IP").is(":checked");
    var IP_MGR = $("#IP_MGR").is(":checked");
    var REP = $("#REP").is(":checked");
    // extra constraints
    var CR = $("#CR").is(":checked");
    var FX = $("#FX").is(":checked");
    var FSD = $("#FSD").is(":checked");
    var scenario = JSON.stringify({
        inputFileName: inputFileName,
        objectiveFunction: objectiveFunction,
        maximization: maximization,
        weights: weights,
        model: model,
        constraints: {
            // general constraints
            RP1: RP1,
            RP2: RP2,
            PF1: PF1,
            PF2: PF2,
            FD1: FD1,
            FD2: FD2,
            FD3: FD3,
            FD4: FD4,
            // additional constraints
            ST: ST,
            SD: SD,
            // specific constraints
            IP: IP,
            IP_MGR: IP_MGR,
            REP: REP,
            // extra constraints
            CR: CR,
            FX: FX,
            FSD: FSD
        }
    });
    return scenario;
}