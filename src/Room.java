import java.nio.channels.SelectionKey;
import java.util.HashSet;

public class Room {
    private String name;

    private HashSet<SelectionKey> Users;

    public Room(String room) {
        this.name = room;
        Users = new HashSet<>();
    }

    /* Getters & Setters */
    public String getName() {
        return this.name;
    }

    public HashSet<SelectionKey> getUsers() {
        return this.Users;
    }

    public void addUser(SelectionKey key) {
        this.Users.add(key);
    }

    public void removeUser(SelectionKey key) {
        this.Users.remove(key);

    }

    public boolean isEmpty(){
        return Users.isEmpty();
    }
}
