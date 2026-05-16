package hu.laci.cms.backend.model.user;

public enum UserSort {
    ID("id"),
    USER_NAME("username"),
    LOGIN_NAME("login_name"),
    EMAIL_ADDRESS("email_address");

    private final String columnName;

    UserSort(String columnName) {
        this.columnName = columnName;
    }

    public String getColumnName() {
        return columnName;
    }
}
