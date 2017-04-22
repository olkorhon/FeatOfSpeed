package fi.semiproot.featofspeed;


import java.io.Serializable;

public class Player implements Serializable{
    private String userId;
    private String nickName;

    public Player(String userId, String nickName) {
        this.userId = userId;
        this.nickName = nickName;
    }

    public String getUserId() {
        return this.userId;
    }

    public String getNickName() {
        return this.nickName;
    }
}
