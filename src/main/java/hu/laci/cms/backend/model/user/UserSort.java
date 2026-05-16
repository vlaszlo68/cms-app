package hu.laci.cms.backend.model.user;

import hu.laci.cms.backend.model.common.BaseSort;

public final class UserSort extends BaseSort {

    public static final UserSort ID = new UserSort(BaseSort.ID.getPropertyName());
    public static final UserSort USER_NAME = new UserSort("userName");
    public static final UserSort LOGIN_NAME = new UserSort("loginName");
    public static final UserSort EMAIL_ADDRESS = new UserSort("emailAddress");

    private UserSort(String propertyName) {
        super(propertyName);
    }
}
