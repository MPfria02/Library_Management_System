package com.librarymanager.backend.entity;

public enum BorrowStatus {
    BORROWED("Borrowed"),
    RETURNED("Returned");    

    private final String displayName;
    
    BorrowStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}