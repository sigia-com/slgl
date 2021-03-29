"use strict"

/**
 * @typedef UserDbItem
 * @property {{values: any[]}} secret_keys
 * @property {number} credits?
 * @property {string} id
 */

/**
 * @property {UserDbItem} item
 */
class UserData {

    constructor(entity) {
        this.entity = entity
    }

    /**
     * @return {boolean}
     */
    hasSecretKey(secretKey) {
        const item = this.entity;
        if (!item.secret_keys || !item.secret_keys.values) {
            return false;
        }
        for (const index in item.secret_keys.values) {
            const buffer = item.secret_keys.values[index];
            const object = JSON.parse(buffer.toString());
            if (object.value === secretKey && !object.disabled) {
                return true;
            }
        }
    }

    /**
     * @return {number}
     */
    get credits() {
        return this.entity.credits || 0;
    }

    hasCreditsLeft() {
        return this.credits > 0
    }
}

/**
 * @type {UserData}
 */
exports.UserData = UserData