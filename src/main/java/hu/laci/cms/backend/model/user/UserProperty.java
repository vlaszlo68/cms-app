package hu.laci.cms.backend.model.user;

import hu.laci.cms.backend.model.common.AuditableProperty;
import hu.laci.cms.backend.model.common.BaseProperty;

public final class UserProperty extends AuditableProperty {

    public static final UserProperty ID = new UserProperty(BaseProperty.ID.getPropertyName());
    public static final UserProperty USER_NAME = new UserProperty("userName");
    public static final UserProperty LOGIN_NAME = new UserProperty("loginName");
    public static final UserProperty EMAIL_ADDRESS = new UserProperty("emailAddress");
    public static final UserProperty ROLE = new UserProperty("role");
    public static final UserProperty ACTIVE = new UserProperty("active");
    public static final UserProperty REGISTRATION_STATE = new UserProperty("registrationState");
    public static final UserProperty CREATED_AT = new UserProperty(AuditableProperty.CREATED_AT.getPropertyName());
    public static final UserProperty UPDATED_AT = new UserProperty(AuditableProperty.UPDATED_AT.getPropertyName());
    public static final UserProperty CREATED_BY = new UserProperty(AuditableProperty.CREATED_BY.getPropertyName());
    public static final UserProperty UPDATED_BY = new UserProperty(AuditableProperty.UPDATED_BY.getPropertyName());

    private UserProperty(String propertyName) {
        super(User.class, propertyName);
    }
}
