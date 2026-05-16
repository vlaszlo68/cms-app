package hu.laci.cms.backend.model.user;

import hu.laci.cms.backend.dao.common.FilterOperation;
import hu.laci.cms.backend.dao.common.FilterProperty;
import hu.laci.cms.backend.model.common.BaseFilter;

public class UserFilter extends BaseFilter {

    @FilterProperty(entityProperty = "userName", operation = FilterOperation.LIKE)
    private String userName;

    @FilterProperty(entityProperty = "loginName", operation = FilterOperation.LIKE)
    private String loginName;

    @FilterProperty(entityProperty = "emailAddress", operation = FilterOperation.LIKE)
    private String emailAddress;

    public UserFilter() {
    }

    public UserFilter(String userName, String loginName, String emailAddress) {
        this(null, userName, loginName, emailAddress);
    }

    public UserFilter(Long id, String userName, String loginName, String emailAddress) {
        super(id);
        this.userName = userName;
        this.loginName = loginName;
        this.emailAddress = emailAddress;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }
}
