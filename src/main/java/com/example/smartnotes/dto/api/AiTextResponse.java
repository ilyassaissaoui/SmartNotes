package com.example.smartnotes.dto.api;

public class AiTextResponse {

    private String title;
    private String text;
    private boolean fromCache;

    public AiTextResponse() {
    }

    public AiTextResponse(String title, String text, boolean fromCache) {
        this.title = title;
        this.text = text;
        this.fromCache = fromCache;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isFromCache() {
        return fromCache;
    }

    public void setFromCache(boolean fromCache) {
        this.fromCache = fromCache;
    }
}
