package com.orman.backend.authorization.service;

public interface OwnerProtectionService {

    void assertCanDeactivateUser(String login);

    void assertCanDeactivatePerson(Integer codper);

    void assertCanRemoveAssignment(String login, Integer codr);

    void assertCanModifyProtectedRole(Integer codr);
}
