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
            setInterval(updateScroll, 3000);
        }
        return message;
    }
    catch (e) {
        return 0;
    }
}

function updateOutput() {
    var output = getOutput();
    for (var s = 0; s < output.length; s++) {
        document.getElementById("output").innerText += output[s];
    }
}

var scrolled = false;
function updateScroll() {
    if (!scrolled) {
        var element = document.getElementById("outputDiv");
        element.scrollTop = element.scrollHeight;
    }
}

$("#outputDiv").on('scroll', function () {
    scrolled = true;
});

$("#run-button").click(function () {
    scrolled = false;
});

function getOutput() {
    try {
        var message = null;
        $.ajax
        ({
            url: "output",
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