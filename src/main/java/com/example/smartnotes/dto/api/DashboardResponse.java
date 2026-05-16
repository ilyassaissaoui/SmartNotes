package com.example.smartnotes.dto.api;

import java.util.List;

public class DashboardResponse {

    private long noteCount;
    private List<NoteResponse> latestNotes;

    public DashboardResponse() {
    }

    public DashboardResponse(long noteCount, List<NoteResponse> latestNotes) {
        this.noteCount = noteCount;
        this.latestNotes = latestNotes;
    }

    public long getNoteCount() {
        return noteCount;
    }

    public void setNoteCount(long noteCount) {
        this.noteCount = noteCount;
    }

    public List<NoteResponse> getLatestNotes() {
        return latestNotes;
    }

    public void setLatestNotes(List<NoteResponse> latestNotes) {
        this.latestNotes = latestNotes;
    }
}
