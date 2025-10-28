package ca.ualberta.codarc.codarc_events.model;

public class OrganizerEventCard {
    private String title;
    private String dates;
    private String registrationStatus;
    private String registrationCloses;
    private String eventId;

    public OrganizerEventCard() {}

    public OrganizerEventCard(String title, String dates, String registrationStatus, String registrationCloses, String eventId) {
        this.title = title;
        this.dates = dates;
        this.registrationStatus = registrationStatus;
        this.registrationCloses = registrationCloses;
        this.eventId = eventId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDates() {
        return dates;
    }

    public void setDates(String dates) {
        this.dates = dates;
    }

    public String getRegistrationStatus() {
        return registrationStatus;
    }

    public void setRegistrationStatus(String registrationStatus) {
        this.registrationStatus = registrationStatus;
    }

    public String getRegistrationCloses() {
        return registrationCloses;
    }

    public void setRegistrationCloses(String registrationCloses) {
        this.registrationCloses = registrationCloses;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
}
