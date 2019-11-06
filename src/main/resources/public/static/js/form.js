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
    if (model === "dimensioning") {
        document.getElementById("PF3").checked = false;
    } else {
        document.getElementById("PF3").checked = true;
    }
    if (model === "dimensioning" || model === "init" || model === "mgr") {
        document.getElementById("single-path").checked = true;
        document.getElementById("fix-init-plc").checked = false;
    }
    if (model === "rep") {
        document.getElementById("single-path").checked = false;
        document.getElementById("fix-init-plc").checked = true;
    }
    if (model === "mgrep") {
        document.getElementById("single-path").checked = false;
        document.getElementById("fix-init-plc").checked = false;
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
    // general
    var RP1 = $("#RP1").is(":checked");
    var RP2 = $("#RP2").is(":checked");
    var PF1 = $("#PF1").is(":checked");
    var PF2 = $("#PF2").is(":checked");
    var PF3 = $("#PF3").is(":checked");
    var FD1 = $("#FD1").is(":checked");
    var FD2 = $("#FD2").is(":checked");
    var FD3 = $("#FD3").is(":checked");
    var FD4 = $("#FD4").is(":checked");
    // additional
    var sync_traffic = $("#sync-traffic").is(":checked");
    var serv_delay = $("#serv-delay").is(":checked");
    var only_cloud = $("#only-cloud").is(":checked");
    var only_edge = $("#only-edge").is(":checked");
    // other
    var single_path = $("#single-path").is(":checked");
    var set_init_plc = $("#set-init-plc").is(":checked");
    var force_src_dst = $("#force-src-dst").is(":checked");
    var const_rep = $("#const-rep").is(":checked");
    var scenario = JSON.stringify({
        inputFileName: inputFileName,
        objectiveFunction: objectiveFunction,
        maximization: maximization,
        weights: weights,
        model: model,
        constraints: {
            // general
            RP1: RP1,
            RP2: RP2,
            PF1: PF1,
            PF2: PF2,
            PF3: PF3,
            FD1: FD1,
            FD2: FD2,
            FD3: FD3,
            FD4: FD4,
            // additional
            sync_traffic: sync_traffic,
            serv_delay: serv_delay,
            only_cloud: only_cloud,
            only_edge: only_edge,
            // other
            single_path: single_path,
            set_init_plc: set_init_plc,
            force_src_dst: force_src_dst,
            const_rep: const_rep
        }
    });
    return scenario;
}