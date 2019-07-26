import java.sql.*;
import java.util.Arrays;
public class DatabaseManager {


    private Statement stmt;
    private Connection conn;
    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://localhost/messagingappusers";
    static final String USER = "root";
    static final String PASS = null;
    static final String DB_Name = "Users";

    public DatabaseManager() throws Exception{
        Class.forName("com.mysql.cj.jdbc.Driver");
        conn = DriverManager.getConnection(DB_URL, USER, PASS);
        stmt = conn.createStatement();

    }

    public boolean addNewUser(String username, byte[] passwordHash, byte[] passwordSalt, int privilege) throws  Exception{

        String insertNewUserString;
        PreparedStatement insertNewUserPS;

        if(doesUserExist(username)){
            return false;
        }
        else{
            insertNewUserString = "INSERT INTO " + DB_Name + "(Username, PasswordHash, PasswordSalt, Privilege)" + " VALUES (?, ?, ?, ?)";
            insertNewUserPS = conn.prepareStatement(insertNewUserString);

            insertNewUserPS.setString(1,username);
            insertNewUserPS.setBytes(2,passwordHash);
            insertNewUserPS.setBytes(3,passwordSalt);
            insertNewUserPS.setInt(4,privilege);
            insertNewUserPS.executeUpdate();
            insertNewUserPS.clearParameters();

            return true;
        }

    }

    public boolean doesUserExist(String username)throws Exception{

        String sql = "SELECT Username FROM Users";
        ResultSet rst = stmt.executeQuery(sql);

        while(rst.next()){
            if(rst.getString("Username").equals(username)){
                rst.close();
                return true;
            }
        }
        rst.close();
        return false;
    }

    public void removeUser(String user) throws Exception{

        String removeUserString;
        PreparedStatement removeUserPS;

        if(doesUserExist(user)){
            removeUserString = "DELETE FROM " + DB_Name + " WHERE Username= ?" ;
            removeUserPS = conn.prepareStatement(removeUserString);
            removeUserPS.setString(1,user);
            removeUserPS.executeUpdate();
            removeUserPS.clearParameters();
        }
    }

    public byte[] getUserSalt(String user) throws Exception{

        String getUserSaltString;
        PreparedStatement getUserSaltPS;
        byte[] salt;

        if(doesUserExist(user)){
            getUserSaltString = "SELECT PasswordSalt FROM " + DB_Name + " WHERE Username= ?";
            getUserSaltPS = conn.prepareStatement(getUserSaltString);
            getUserSaltPS.setString(1,user);

            ResultSet rst = getUserSaltPS.executeQuery();
            rst.next();
            salt = rst.getBytes("PasswordSalt");

            getUserSaltPS.clearParameters();
            rst.close();
            return salt;
        }
        else{
            return null;
        }
    }

    public int getUserPrivilege(String user) throws Exception{

        String getUserPrivilegeString;
        PreparedStatement getUserPrivilegePS;
        int privilege;
        ResultSet rst;

        if(doesUserExist(user)){
            getUserPrivilegeString = "SELECT Privilege FROM " + DB_Name + " WHERE Username= ?";
            getUserPrivilegePS = conn.prepareStatement(getUserPrivilegeString);
            getUserPrivilegePS.setString(1,user);

            rst = getUserPrivilegePS.executeQuery();
            rst.next();
            privilege = rst.getInt("Privilege");

            rst.close();
            getUserPrivilegePS.clearParameters();
            return privilege;
        }
        else{
            return -1;
        }
    }

    public void setUserPrivilege(String user, int privilege) throws Exception{

        String setUserPrivilegeString;
        PreparedStatement setUserPrivilegePS;

        if(doesUserExist(user)){
           setUserPrivilegeString = "UPDATE " + DB_Name + " SET Privilege= ? WHERE Username= ?";
           setUserPrivilegePS = conn.prepareStatement(setUserPrivilegeString);
           setUserPrivilegePS.setInt(1,privilege);
           setUserPrivilegePS.setString(2,user);

           setUserPrivilegePS.executeUpdate();
           setUserPrivilegePS.clearParameters();
        }
    }

    public boolean verifyPasswordHash(String username, byte[] passwordHash) throws Exception{
        String verifyPasswordHashString;
        PreparedStatement verifyPasswordHashPS;
        ResultSet rst;
        boolean response = false;

        if(doesUserExist(username)){
            verifyPasswordHashString = "SELECT PasswordHash FROM " + DB_Name + " WHERE Username= ?";
            verifyPasswordHashPS = conn.prepareStatement(verifyPasswordHashString);
            verifyPasswordHashPS.setString(1,username);

            rst = verifyPasswordHashPS.executeQuery();
            rst.next();
            response = Arrays.equals(rst.getBytes("PasswordHash"),passwordHash) ;

            rst.close();
            verifyPasswordHashPS.clearParameters();
        }

        return response;
    }

    public void closeDatabaseConnection()throws Exception{
        stmt.close();
        conn.close();
    }

    public static void main(String[] args){
        try{
            DatabaseManager dbm = new DatabaseManager();
            dbm.closeDatabaseConnection();
        }
        catch (Exception e){
            System.out.println(e);
        }


    }


}
