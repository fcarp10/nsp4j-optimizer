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
        if(message!=null) {
            loadLog();
            setTimeout(loadLog, 1000);
        }
        return message;
    }
    catch (e) {
        return 0;
    }
}

function loadLog() {
    var xhttp = new XMLHttpRequest();
    xhttp.onreadystatechange = function () {
        if (this.readyState == 4 && this.status == 200) {
            document.getElementById("log").innerText = this.responseText;
        }
    };
    xhttp.open("GET", "../../mip.log", true);
    xhttp.send();
}