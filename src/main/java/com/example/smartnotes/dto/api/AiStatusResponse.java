package com.example.smartnotes.dto.api;

public class AiStatusResponse {

    private boolean configured;
    private String hint;

    public AiStatusResponse() {
    }

    public AiStatusResponse(boolean configured, String hint) {
        this.configured = configured;
        this.hint = hint;
    }

    public boolean isConfigured() {
        return configured;
    }

    public void setConfigured(boolean configured) {
        this.configured = configured;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }
}
