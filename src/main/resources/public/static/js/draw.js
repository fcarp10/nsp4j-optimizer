var cy;
window.onload = function () {
    initializeGraph();
    updateGraph();
    setInterval(updateGraph, 3000);
};

function initializeGraph() {
    cy = cytoscape({
        container: document.getElementById('cy'),
        boxSelectionEnabled: false,
        autounselectify: true,
        style: cytoscape.stylesheet()
            .selector('node')
            .css({
                'content': 'data(label)',
                'background-color': 'data(faveColor)',
                'border-color': 'data(faveColor)',
                'border-width': 0.5,
                'shape': 'data(faveShape)',
                'width': 'data(width)',
                'height': 'data(height)',
                'font-size': 4,
                'text-valign': 'center',
                'text-halign': 'center',
                'color': '#000',
            })
            .selector('.multiline-manual')
            .css({
                'text-wrap': 'wrap'
            })
            .selector('edge')
            .css({
                'source-label': 'data(label)',
                'source-text-offset': 15,
                'font-size': 5,
                'edge-text-rotation': 'autorotate',
                'text-background-opacity': 1,
                'text-background-color': '#f2f2f2',
                'text-background-shape': 'roundrectangle',
                'width': 1,
                'line-color': 'data(faveColor)',
                'curve-style': 'bezier',
                'target-arrow-shape': 'triangle',
                'target-arrow-color': 'data(faveColor)',
                'arrow-scale': 0.5,
                'control-point-step-size': 10
            })
    });
}

function updateGraph() {
    cy.elements().remove();
    var nodes = getNodes();
    var servers = getServers();
    var links = getLinks();
    for (var n = 0; n < nodes.length; n++) {
        cy.add({
                data: {
                    id: nodes[n]['data']['id'],
                    faveColor: nodes[n]['data']['faveColor'],
                    label: nodes[n]['data']['label'],
                    faveShape: nodes[n]['data']['faveShape'],
                    width: nodes[n]['data']['width'],
                    height: nodes[n]['data']['height']
                },
                position: {
                    x: nodes[n]['position']['x'],
                    y: nodes[n]['position']['y']
                },
                classes: 'multiline-manual'
            }
        );
    }
    for (var x = 0; x < servers.length; x++) {
        cy.add({
                data: {
                    id: servers[x]['data']['id'],
                    faveColor: servers[x]['data']['faveColor'],
                    label: servers[x]['data']['label'],
                    faveShape: servers[x]['data']['faveShape'],
                    width: servers[x]['data']['width'],
                    height: servers[x]['data']['height']
                },
                position: {
                    x: servers[x]['position']['x'],
                    y: servers[x]['position']['y']
                },
                classes: 'multiline-manual'
            }
        );
    }
    for (var l = 0; l < links.length; l++) {
        cy.add({
                data: {
                    id: links[l]['data']['id'],
                    weight: 1,
                    source: links[l]['data']['source'],
                    target: links[l]['data']['target'],
                    label: links[l]['data']['label'],
                    faveColor: links[l]['data']['faveColor']
                }
            }
        );
    }
    cy.layout({
        name: 'preset'
    }).run();
}

function getNodes() {
    try {
        var message = null;
        $.ajax
        ({
            url: "node",
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

function getServers() {
    try {
        var message = null;
        $.ajax
        ({
            url: "server",
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

function getLinks() {
    try {
        var message = null;
        $.ajax
        ({
            url: "link",
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