package com.insight.dashboard.dto;

public class RepositoryRequest {

    private String owner;
    private String repositoryName;
    private String alias;
    private String notes;

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public String getRepositoryName() { return repositoryName; }
    public void setRepositoryName(String repositoryName) { this.repositoryName = repositoryName; }
    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
