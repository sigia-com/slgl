"use strict"
const AWS = require('aws-sdk');
const {UserData} = require('./UserData');

const getDynamoDbClient = (() => {
    let dynamoDb;
    return () => {
        if (!dynamoDb) {
            dynamoDb = new AWS.DynamoDB.DocumentClient();
        }
        return dynamoDb;
    }
})()

const USER_TABLE_NAME = process.env.USER_DATA_DYNAMO_DB_TABLE;


function getUserData(username) {
    return new Promise((resolve, reject) => {

        const params = {
            TableName: USER_TABLE_NAME,
            KeyConditionExpression: 'id = :id',
            ExpressionAttributeValues: {':id': username}
        };

        getDynamoDbClient().query(params, (err, data) => {
            if (err) {
                return reject(err);
            }
            const {Items} = data;
            if (Items.length > 1) {
                return reject(`Too many results for user ${username}`)
            }
            if (Items.length === 0) {
                return reject(`No user with id ${username} found`)
            }

            return resolve(new UserData(Items[0]));

        });
    });
}

/**
 * @return {Promise<UserData>}
 */
exports.getUserData = getUserData;