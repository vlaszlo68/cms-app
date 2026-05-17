package hu.laci.cms.backend.model.user;

import hu.laci.cms.backend.model.common.BaseProperty;

public final class UserProperty extends BaseProperty {

    public static final UserProperty ID = new UserProperty("id");
    public static final UserProperty USER_NAME = new UserProperty("userName");
    public static final UserProperty LOGIN_NAME = new UserProperty("loginName");
    public static final UserProperty EMAIL_ADDRESS = new UserProperty("emailAddress");

    private UserProperty(String propertyName) {
        super(User.class, propertyName);
    }
}
