"use strict"

class AuthorizationCallback {

    constructor(event, callback) {
        this.event = event;
        this.callback = callback;
    }

    allow(userId, credits) {
        this.callback(null, generatePolicy('user', 'Allow', this.event.methodArn, userId, credits));
    }

    deny() {
        this.callback('Unauthorized');
    }
}

function generatePolicy(principalId, effect, resource, userId, credits) {
    const authResponse = {};

    authResponse.principalId = principalId;
    if (effect && resource) {
        const policyDocument = {};
        policyDocument.Version = '2012-10-17';
        policyDocument.Statement = [];
        const statementOne = {};
        statementOne.Action = 'execute-api:Invoke';
        statementOne.Effect = effect;
        statementOne.Resource = resource;
        policyDocument.Statement[0] = statementOne;
        authResponse.policyDocument = policyDocument;
    }
    authResponse.context = {
        "user_id": userId,
        "credits": credits
    };
    return authResponse;
}

exports.AuthorizationCallback = AuthorizationCallback
