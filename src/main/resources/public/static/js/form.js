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
            document.getElementById("num_servers").checked = false;
            document.getElementById("single_path").checked = true;
            document.getElementById("fix_init_plc").checked = false;
            document.getElementById("objectiveFunction").getElementsByTagName('option')[0].selected = "selected";
        }
    if (model === "init") {
        document.getElementById("num_servers").checked = true;
        document.getElementById("single_path").checked = true;
        document.getElementById("fix_init_plc").checked = false;
        document.getElementById("objectiveFunction").getElementsByTagName('option')[2].selected = "selected";
    }
    else if (model === "mgr") {
        document.getElementById("num_servers").checked = false;
        document.getElementById("single_path").checked = true;
        document.getElementById("fix_init_plc").checked = false;
        document.getElementById("objectiveFunction").getElementsByTagName('option')[3].selected = "selected";
    }
    else if (model === "rep") {
        document.getElementById("num_servers").checked = false;
        document.getElementById("single_path").checked = false;
        document.getElementById("fix_init_plc").checked = true;
        document.getElementById("objectiveFunction").getElementsByTagName('option')[3].selected = "selected";;
    }
    else if (model === "mgrep") {
        document.getElementById("num_servers").checked = false;
        document.getElementById("single_path").checked = false;
        document.getElementById("fix_init_plc").checked = false;
        document.getElementById("objectiveFunction").getElementsByTagName('option')[3].selected = "selected";
    }
    else if (model === "mgrep_rl") {
        document.getElementById("objectiveFunction").getElementsByTagName('option')[3].selected = "selected";
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
    var num_servers = $("#num_servers").is(":checked");
    var single_path = $("#single_path").is(":checked");
    var fix_init_plc = $("#fix_init_plc").is(":checked");
    // extra constraints
    var const_rep = $("#const_rep").is(":checked");
    var fix_src_dst = $("#fix_src_dst").is(":checked");
    var use_cloud = $("#use_cloud").is(":checked");
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
            num_servers: num_servers,
            single_path: single_path,
            fix_init_plc: fix_init_plc,
            // extra constraints
            const_rep: const_rep,
            fix_src_dst: fix_src_dst,
            use_cloud: use_cloud
        }
    });
    return scenario;
}