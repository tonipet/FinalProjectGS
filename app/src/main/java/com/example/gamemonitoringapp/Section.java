package com.example.gamemonitoringapp;

public class Section {
    private String sectionName;

    public Section() {
        // Default constructor required for calls to DataSnapshot.getValue(Section.class)
    }

    public Section(String sectionName) {
        this.sectionName = sectionName;
    }

    public String getSectionName() {
        return sectionName;
    }

    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
    }
}
