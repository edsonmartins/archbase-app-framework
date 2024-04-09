package br.com.archbase.validation.constraints;


public enum ParseableType {

    TO_SHORT("Short"),
    TO_INT("Integer"),
    TO_LONG("Long"),
    TO_DOUBLE("Double"),
    TO_FLOAT("Float");
    private String friendlyName;

    private ParseableType(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

}
