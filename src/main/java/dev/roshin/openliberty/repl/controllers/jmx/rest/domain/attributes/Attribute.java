package dev.roshin.openliberty.repl.controllers.jmx.rest.domain.attributes;

public class Attribute {
    //    @SerializedName("name")
    private String name;
    //    @SerializedName("value")
    private AttributeValue value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AttributeValue getValue() {
        return value;
    }

    public void setValue(AttributeValue value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Attribute{" +
                "name='" + name + '\'' +
                ", value=" + value +
                '}';
    }
}
