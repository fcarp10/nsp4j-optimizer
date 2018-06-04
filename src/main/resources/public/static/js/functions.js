function startLinkOpt() {
    try {
        var message = null;
        $.ajax
        ({
            url: "link-opt",
            type: "GET",
            async: false,
            success: function (ans) {
                message = ans;
            }
        });
        if (message != null) {
            setInterval(updateOutput, 3000);
        }
        return message;
    }
    catch (e) {
        return 0;
    }
}

function updateOutput() {
    var results = getResults();
    if (results != null) {
        document.getElementById("lu").innerText = results['avgLu'] + ' - ' + results['minLu'] + ' - ' + results['maxLu'] + ' - ' + results['vrcLu'];
        document.getElementById("xu").innerText = results['avgXu'] + ' - ' + results['minXu'] + ' - ' + results['maxXu'] + ' - ' + results['vrcXu'];
        document.getElementById("path").innerText = results['avgPathLength'];
        document.getElementById("cost").innerText = results['cost'];
    } else {
        document.getElementById("lu").innerText = "0.0 - 0.0 - 0.0 - 0.0 ";
        document.getElementById("xu").innerText = "0.0 - 0.0 - 0.0 - 0.0 ";
        document.getElementById("path").innerText = "0.0";
        document.getElementById("cost").innerText = "0.0";
    }
    updateMessage();
}

function updateMessage() {
    var message = getMessage();
    if (message != null)
        document.getElementById("message").innerText = message;
    else
        document.getElementById("message").innerText = "";
}

function getResults() {
    try {
        var message = null;
        $.ajax
        ({
            url: "results",
            type: "GET",
            async: false,
            success: function (ans) {
                message = ans;
            }
        });
        return message;
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
        return message;
    }
    catch (e) {
        return 0;
    }
}