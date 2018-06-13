var refreshIntervalId;
function startOpt() {
    document.getElementById("message").innerText = "";
    var runMessage = document.getElementById("opt").value + "-" + document.getElementById("obj").value;
    try {
        var message = null;
        $.ajax
        ({
            data: runMessage,
            url: "run",
            type: "POST",
            async: false,
            success: function (ans) {
                message = ans;
            }
        });
        if (message != null) {
            document.getElementById("message").innerText = message;
            setInterval(getResults, 1000);
            refreshIntervalId = setInterval(getMessage, 1000);
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
            document.getElementById("message").innerText = "";
            clearInterval(refreshIntervalId);
        }
    }
    catch (e) {
        return 0;
    }
}