package com.epam.dlab.backendapi.dao;

/**
 * Created by Alexey Suprun
 */
public interface MongoCollections {
    String LOGIN_ATTEMPTS = "loginAttempts";
    String DOCKER_ATTEMPTS = "dockerAttempts";
    String USER_KEYS = "userKeys";
}
