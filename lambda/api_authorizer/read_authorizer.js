"use strict"
const {AuthorizationCallback} = require("./AuthorizationCallback");
const {verifyAuthorizationCredentials} = require("./authorizer/credentialsAuthorizer");
const {verifyAnonymousAccess} = require("./authorizer/anonymousAuthorizer");

exports.handler = function handler(event, context, callback) {
    const authCallback = new AuthorizationCallback(event, callback);
    const authorizationHeader = getAuthorizationHeader(event);

    if (!!authorizationHeader) {
        parseAuthorizationHeader(authorizationHeader)
            .then(parsed => verifyAuthorizationCredentials(parsed, authCallback))
            .catch(() => authCallback.deny())
    } else {
        verifyAnonymousAccess(event, authCallback);
    }
};

function getAuthorizationHeader(event) {
    const headers = event.headers || {};

    for (let header of Object.keys(headers)) {
        if (header.toLowerCase() === 'authorization') {
            return headers[header];
        }
    }
    return null;
}

function parseAuthorizationHeader(authorizationHeader) {
    if (!authorizationHeader.startsWith("Basic ")) {
        return Promise.reject("Invalid authentication header");
    }
    const buf = Buffer.from(authorizationHeader.split(' ')[1], 'base64');
    const authString = buf.toString();
    const [usernameEncoded, secretKey] = authString.split(':');
    const username = decodeURIComponent(usernameEncoded);
    return Promise.resolve({username, secretKey});
}



