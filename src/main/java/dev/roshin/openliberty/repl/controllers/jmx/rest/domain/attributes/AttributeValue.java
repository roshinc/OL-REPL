package dev.roshin.openliberty.repl.controllers.jmx.rest.domain.attributes;

public class AttributeValue {
    //    @SerializedName("value")
    private String value;
    //    @SerializedName("type")
    private String type;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "AttributeValue{" +
                "value='" + value + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
