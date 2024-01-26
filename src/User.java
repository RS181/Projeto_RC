
enum State {
    init, outside, inside
}

public class User {
    private String message = "";

    private String nick;

    private Room chatRoom;

    private State chatState;

    public User() {
        this.chatState = State.init;
        this.nick = "";
    }

    public void cleanMessage() {
        this.message = "";
    }

    public boolean isInRoom() {
        if (this.chatRoom == null) {
            return false;
        }
        return true;
    }

    /* Getters & Setters */

    public String getMessage() {
        return message;
    }

    public void addMsg(String msg) {
        this.message = this.message + msg;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public Room getChatRoom() {
        return this.chatRoom;
    }

    public void setChatRoom(Room chatRoom) {
        this.chatRoom = chatRoom;
    }

    public State getChatState() {
        return this.chatState;
    }

    public void setChatState(State chatState) {
        this.chatState = chatState;
    }

}
