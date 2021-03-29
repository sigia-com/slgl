'use strict'
const userDataService = require("../userdata/userDataService");

/**
 *
 * @param {{username, secretKey}}authHeader
 * @param {AuthorizationCallback} authCallback
 */
exports.verifyAuthorizationCredentials = (authHeader, authCallback) => {
    const {username, secretKey} = authHeader;

    userDataService.getUserData(username)
        .then(user => {
            if (user.hasSecretKey(secretKey)) {
                authCallback.allow(username, user.credits);
            } else {
                authCallback.deny();
            }
        })
        .catch(err => {
            console.error('Error when trying to authorize: ' + err);
            authCallback.deny();
        });
}