package com.librarymanager.backend.entity;

public enum BookGenre {
    FICTION("Fiction"),
    NON_FICTION("Non-Fiction"),
    SCIENCE("Science"),
    TECHNOLOGY("Technology"),
    HISTORY("History"),
    BIOGRAPHY("Biography"),
    MYSTERY("Mystery"),
    ROMANCE("Romance"),
    FANTASY("Fantasy");
    
    private final String displayName;
    
    BookGenre(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}