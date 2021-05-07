var cy;
var servers;
var links;
initializeGraph();

function initializeGraph(cyContainer) {
    cy = cytoscape({
        container: document.getElementById(cyContainer),
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
                'font-size': 12,
                'text-valign': 'center',
                'text-halign': 'center',
                'color': 'white',
            })
            .selector('.multiline-manual')
            .css({
                'text-wrap': 'wrap'
            })
            .selector('edge')
            .css({
                'source-label': 'data(label)',
                'source-text-offset': 15,
                'font-size': 6,
                'edge-text-rotation': 'autorotate',
                'color': 'Gray',
                'text-background-opacity': 1,
                'text-background-color': '#E9ECEF',
                'text-background-shape': 'roundrectangle',
                'width': 2,
                'line-color': 'data(faveColor)',
                'curve-style': 'bezier',
                'target-arrow-shape': 'triangle',
                'target-arrow-color': 'data(faveColor)',
                'arrow-scale': 0.5,
                'control-point-step-size': 10
            })
    });
    servers = getServers();
    links = getLinks();
    nodes = getNodes()
    cy.add(nodes);
    cy.add(servers);
    cy.add(links);
    cy.layout({
        name: 'preset'
    }).run();
}

function areEqual(obj1, obj2) {
    if (obj1['data']['faveColor'] !== obj2['data']['faveColor']) {
        return false;
    }
    if (obj1['data']['label'] !== obj2['data']['label']) {
        return false;
    }
    return true;
}

function updateGraph() {
    var updatedServers = getServers();
    var updatedLinks = getLinks();
    var isChange = false;
    for (var x = 0; x < updatedServers.length; x++) {
        if (!areEqual(updatedServers[x], servers[x])) {
            servers = updatedServers;
            isChange = true;
            break;
        }
    }
    for (var l = 0; l < links.length; l++) {
        if (!areEqual(updatedLinks[l], links[l])) {
            links = updatedLinks;
            isChange = true;
            break;
        }
    }
    if (isChange) {
        cy.batch(function () {
            for (var x = 0; x < servers.length; x++) {
                cy.getElementById(servers[x]['data']['id'])
                    .data("faveColor", servers[x]['data']['faveColor'])
                    .data("label", servers[x]['data']['label'])
                    ;
            }
            for (var l = 0; l < links.length; l++) {
                cy.getElementById(links[l]['data']['id'])
                    .data("faveColor", links[l]['data']['faveColor'])
                    .data("label", links[l]['data']['label'])
                    ;
            }
        });
        cy.layout.run();
    }
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