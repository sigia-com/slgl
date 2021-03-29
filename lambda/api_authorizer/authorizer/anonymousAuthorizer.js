'use strict'

exports.verifyAnonymousAccess = (event, authCallback) => {
    if (isAnonymousAccessAllowed(event.requestContext.httpMethod, event.path)) {
        authCallback.allow()
    } else {
        authCallback.deny();
    }
}

function isAnonymousAccessAllowed(method, path) {
    return method === 'OPTIONS';
}