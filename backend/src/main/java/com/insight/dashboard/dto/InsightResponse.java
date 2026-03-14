package com.insight.dashboard.dto;

import java.util.List;

public class InsightResponse {

    private String overview;
    private String purpose;
    private String complexity;
    private List<String> technologies;
    private List<String> improvements;

    public String getOverview() { return overview; }
    public void setOverview(String overview) { this.overview = overview; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getComplexity() { return complexity; }
    public void setComplexity(String complexity) { this.complexity = complexity; }
    public List<String> getTechnologies() { return technologies; }
    public void setTechnologies(List<String> technologies) { this.technologies = technologies; }
    public List<String> getImprovements() { return improvements; }
    public void setImprovements(List<String> improvements) { this.improvements = improvements; }
}
