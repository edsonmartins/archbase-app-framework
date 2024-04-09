package br.com.archbase.ddd.infraestructure.persistence.jpa.repository;

public class SimpleMultipleFieldsFilter {

    private String search;
    private String fields;

    public SimpleMultipleFieldsFilter() {
    }

    public SimpleMultipleFieldsFilter(String search, String fields) {
        this.search = search;
        this.fields = fields;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public String getFields() {
        return fields;
    }

    public void setFields(String fields) {
        this.fields = fields;
    }
}
