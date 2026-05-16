package com.example.smartnotes.dto.api;

import java.util.List;

public class AiListResponse {

    private String title;
    private List<String> items;
    private boolean fromCache;

    public AiListResponse() {
    }

    public AiListResponse(String title, List<String> items, boolean fromCache) {
        this.title = title;
        this.items = items;
        this.fromCache = fromCache;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getItems() {
        return items;
    }

    public void setItems(List<String> items) {
        this.items = items;
    }

    public boolean isFromCache() {
        return fromCache;
    }

    public void setFromCache(boolean fromCache) {
        this.fromCache = fromCache;
    }
}
