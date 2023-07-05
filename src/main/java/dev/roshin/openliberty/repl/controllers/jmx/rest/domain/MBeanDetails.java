package dev.roshin.openliberty.repl.controllers.jmx.rest.domain;

import java.util.List;

public class MBeanDetails {
    public String className;
    public String description;
    public Descriptor descriptor;
    public List<Object> attributes;
    public String attributes_URL;
    public List<Constructor> constructors;
    public List<Object> notifications;
    public List<Operation> operations;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Descriptor getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(Descriptor descriptor) {
        this.descriptor = descriptor;
    }

    public List<Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<Object> attributes) {
        this.attributes = attributes;
    }

    public String getAttributes_URL() {
        return attributes_URL;
    }

    public void setAttributes_URL(String attributes_URL) {
        this.attributes_URL = attributes_URL;
    }

    public List<Constructor> getConstructors() {
        return constructors;
    }

    public void setConstructors(List<Constructor> constructors) {
        this.constructors = constructors;
    }

    public List<Object> getNotifications() {
        return notifications;
    }

    public void setNotifications(List<Object> notifications) {
        this.notifications = notifications;
    }

    public List<Operation> getOperations() {
        return operations;
    }

    public void setOperations(List<Operation> operations) {
        this.operations = operations;
    }

    public class Signature {
        public String name;
        public String type;
        public String description;
        public Descriptor descriptor;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Descriptor getDescriptor() {
            return descriptor;
        }

        public void setDescriptor(Descriptor descriptor) {
            this.descriptor = descriptor;
        }
    }

    public class Value {
        public String value;
        public String type;

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
    }

    public class Constructor {
        public String name;
        public String description;
        public Descriptor descriptor;
        public List<Object> signature;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Descriptor getDescriptor() {
            return descriptor;
        }

        public void setDescriptor(Descriptor descriptor) {
            this.descriptor = descriptor;
        }

        public List<Object> getSignature() {
            return signature;
        }

        public void setSignature(List<Object> signature) {
            this.signature = signature;
        }
    }

    public class Descriptor {
        public List<String> names;
        public List<Value> values;

        public List<String> getNames() {
            return names;
        }

        public void setNames(List<String> names) {
            this.names = names;
        }

        public List<Value> getValues() {
            return values;
        }

        public void setValues(List<Value> values) {
            this.values = values;
        }
    }

    public class Operation {
        public String name;
        public String description;
        public Descriptor descriptor;
        public String impact;
        public String returnType;
        public List<Signature> signature;
        public String URL;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Descriptor getDescriptor() {
            return descriptor;
        }

        public void setDescriptor(Descriptor descriptor) {
            this.descriptor = descriptor;
        }

        public String getImpact() {
            return impact;
        }

        public void setImpact(String impact) {
            this.impact = impact;
        }

        public String getReturnType() {
            return returnType;
        }

        public void setReturnType(String returnType) {
            this.returnType = returnType;
        }

        public List<Signature> getSignature() {
            return signature;
        }

        public void setSignature(List<Signature> signature) {
            this.signature = signature;
        }

        public String getURL() {
            return URL;
        }

        public void setURL(String URL) {
            this.URL = URL;
        }
    }
}