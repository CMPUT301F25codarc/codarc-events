package ca.ualberta.codarc.codarc_events.model;

public class EventCard {
    private String title;
    private String location;
    private String lotteryEnds;
    private String entrantsInfo;
    private boolean isJoined;

    public EventCard() {}

    public EventCard(String title, String location, String lotteryEnds, String entrantsInfo, boolean isJoined) {
        this.title = title;
        this.location = location;
        this.lotteryEnds = lotteryEnds;
        this.entrantsInfo = entrantsInfo;
        this.isJoined = isJoined;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getLotteryEnds() {
        return lotteryEnds;
    }

    public void setLotteryEnds(String lotteryEnds) {
        this.lotteryEnds = lotteryEnds;
    }

    public String getEntrantsInfo() {
        return entrantsInfo;
    }

    public void setEntrantsInfo(String entrantsInfo) {
        this.entrantsInfo = entrantsInfo;
    }

    public boolean isJoined() {
        return isJoined;
    }

    public void setJoined(boolean joined) {
        isJoined = joined;
    }
}
