import java.security.PublicKey;

public class User implements java.io.Serializable{

    private String userName;
    private PublicKey publicKey;

    //User objects consist of a username and the users publickey
    //they are sent directly to the server upon creation of the client and distributed by the server

    public User(String userName, PublicKey publicKey) {
        this.userName = userName;
        this.publicKey = publicKey;
    }

    public String getUserName() {
        return userName;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }
}
