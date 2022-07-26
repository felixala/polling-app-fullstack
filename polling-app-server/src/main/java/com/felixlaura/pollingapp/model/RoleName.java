package com.felixlaura.pollingapp.model;

/**
 * Whenever a user logs in, we will assign ROLE_USER as default.
 * To Assign roles, they should be present in the database.
*/
public enum RoleName {
    ROLE_USER,
    ROLE_ADMIN
}
