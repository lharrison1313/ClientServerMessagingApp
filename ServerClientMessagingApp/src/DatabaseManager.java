import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;

/*
    Purpose: The purpose of this class is to manage the mysql database which contains all server and user information for
    the ServerClientMessagingApp. The methods in this class use prepared statements in order to help mitigate sql injection
    attacks.
 */

public class DatabaseManager {

    private Connection conn;
    static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://localhost/MESSAGING_APP";
    static final String USER = "root";
    static final String PASS = null;
    static final String SERVER_TABLE = "SERVERS";

    //initializes the database manager object and creates a new database if specified
    public DatabaseManager(boolean newDatabase)throws Exception{
        if(newDatabase){
            createNewDatabase();
        }
        Class.forName(JDBC_DRIVER);
        conn = DriverManager.getConnection(DB_URL, USER, PASS);
        if(newDatabase){
            createServerTable();
        }
    }

    //Creates a new database called Messaging_App on the mysql server
    public void createNewDatabase() throws Exception{
        Connection firstConn = DriverManager.getConnection("jdbc:mysql://localhost/",USER,PASS);
        String sql = "CREATE DATABASE MESSAGING_APP";
        Statement stmt = firstConn.createStatement();
        stmt.executeUpdate(sql);
        stmt.close();
        firstConn.close();

    }

    /*
        createServerTable: creates a new table called Servers in the Messaging_App database. This table is
        used for storing multiple servers and information pertaining to these servers. This information includes
        the following items:

        Servername - the name of the server
        PasswordHash - the hash of the servers password
        PasswordSalt - the randomly generated salt that will be added to the servers password when it is hashed
        UserTable - the name of the table on the database which stores this servers user info
        ServerAccessPasswordHash (SAP) - the hash of the servers access password used by clients to gain access to the server
        ServerAccessPasswordSalt - the salt for the (SAP)
     */
    public void createServerTable() throws Exception{
        String sqlServersTable = "CREATE TABLE SERVERS (Servername varchar(20), PasswordHash varbinary(512), PasswordSalt varbinary(32), UserTable varchar(20), ServerAccessPasswordHash varbinary(512), ServerAccessPasswordSalt varbinary(32))";
        Statement stmt = conn.createStatement();
        stmt.executeUpdate(sqlServersTable);
        stmt.close();
    }

    /*
        addNewServer: This method adds a new server to the server table using the items described in the
        previous method as parameters.
     */
    public void addNewServer(String servername, byte[] passwordHash, byte[] passwordSalt, byte[] serverAccessPasswordHash, byte[] serverAccessPasswordSalt) throws Exception{

        Statement stmt = conn.createStatement();
        ResultSet rst = stmt.executeQuery("SELECT COUNT(*) FROM SERVERS");
        rst.next();
        int userTableNO = rst.getInt(1);
        String userTableName = "Users" + userTableNO;

        createUserTable(userTableName);

        String insertNewServerString = "INSERT INTO " + SERVER_TABLE + "(Servername, PasswordHash, PasswordSalt, UserTable, ServerAccessPasswordHash, ServerAccessPasswordSalt)" + " VALUES (?,?,?,?,?,?)";

        PreparedStatement insertNewSeverPS = conn.prepareStatement(insertNewServerString);


        insertNewSeverPS.setString(1,servername);
        insertNewSeverPS.setBytes(2,passwordHash);
        insertNewSeverPS.setBytes(3,passwordSalt);
        insertNewSeverPS.setString(4,userTableName);
        insertNewSeverPS.setBytes(5,serverAccessPasswordHash);
        insertNewSeverPS.setBytes(6,serverAccessPasswordSalt);

        insertNewSeverPS.executeUpdate();


        insertNewSeverPS.clearParameters();
    }

    /*
        createUserTable: creates a new user table for a specific server. This table is used for storing all of the servers users and their
        information. This information includes the following items:

        Username - the name of the user
        PasswordHash - the hash of the users password
        PasswordSalt - the randomly generated salt that is added to the users password when it is hashed
        Privilege - this represents what a user can and cannot do on the server
     */
    public void createUserTable(String userTableName) throws Exception{
        String createUsersTableString = "CREATE TABLE " + userTableName + " (Username varchar(20), PasswordHash varbinary(512), PasswordSalt varbinary(32), Privilege int(100))";
        PreparedStatement createUserTablePS = conn.prepareStatement(createUsersTableString);
        createUserTablePS.executeUpdate();
        createUserTablePS.clearParameters();
    }

    /*
        addNewUser: this method adds a new user to the servers user table using the items described in the previous method as a
        parameters.

        @parameter serverName: this is the name of the server which the user belongs to

        @return this method return true if the server the user belongs to exists and it was successfully added
     */
    public boolean addNewUser( String serverName, String username, byte[] passwordHash, byte[] passwordSalt, int privilege) throws  Exception{

        String insertNewUserString;
        PreparedStatement insertNewUserPS;

        if(doesUserExist(serverName,username)){
            return false;
        }
        else{
            insertNewUserString = "INSERT INTO " +  getServersUserTable(serverName) + "(Username, PasswordHash, PasswordSalt, Privilege)" + " VALUES (?, ?, ?, ?)";
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

    /*
        doesUserExist: this method checks if a user exists on a server currently

        @param serverName: the name of the server which the user belongs
        @param username: the name of the user that needs to be checked

        @return the method returns true if the user exists
     */
    public boolean doesUserExist(String serverName, String username)throws Exception{

        Statement stmt = conn.createStatement();
        String sql = "SELECT Username FROM " + getServersUserTable(serverName);

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


    /*
        doesServerExist: This method checks if the server table contains a specific server.

        @param serverName: this is the name of the server being searched for

        @return this method returns true if the server exists
     */
    public boolean doesServerExist(String serverName) throws Exception{
        Statement stmt = conn.createStatement();
        String sql = "SELECT Servername FROM " + SERVER_TABLE;

        ResultSet rst = stmt.executeQuery(sql);

        while(rst.next()){
            if(rst.getString("Servername").equals(serverName)){
                rst.close();
                return true;
            }
        }
        rst.close();
        return false;
    }

    /*
        getServersUserTable: This method attempts to retrieve the name of the user table which
        belongs to a specific server.

        @param serverName: The name of the server for which you wish to retrieve its user table.

        @return this method returns the name of the user table for a specific server
     */
    public String getServersUserTable(String serverName)throws Exception{

        if(doesServerExist(serverName)) {
            String getServersUserTableString = "SELECT UserTable FROM Servers WHERE Servername=? ";
            PreparedStatement getServersUserTablePS = conn.prepareStatement(getServersUserTableString);
            getServersUserTablePS.setString(1, serverName);
            ResultSet rst = getServersUserTablePS.executeQuery();
            rst.next();
            String UserTableName = rst.getString("UserTable");
            rst.close();
            return UserTableName;
        }
        else{
            return null;
        }
    }

    /*
        removeUser: this method removes a user from a specific server

        @param serverName: the server which the user belongs to
        @param user: the user which you wish to remove

        @return this method returns true if the user was successfully removed from the server table
     */
    public boolean removeUser(String serverName, String user) throws Exception{

        String removeUserString;
        PreparedStatement removeUserPS;
        boolean userRemoved = false;

        if(doesUserExist(serverName, user)){
            removeUserString = "DELETE FROM " + getServersUserTable(serverName) + " WHERE Username= ?" ;
            removeUserPS = conn.prepareStatement(removeUserString);
            removeUserPS.setString(1,user);
            removeUserPS.executeUpdate();
            removeUserPS.clearParameters();
            userRemoved = true;
        }
        return userRemoved;

    }

    /*
        getUserSalt: This method attempts to retrieve a user's or server's password salt.

        @param serverName: the name of the server the user belongs to.
        @param name:  username who's salt you wish to receive if it is a server leave this null.
        @param isServer: if you wish to recieve the server's password salt set to true.

        @return this method returns a byte array of the server's or user's password salt. If the
        user or server does not exist then it will return a null byte array.
     */
    public byte[] getUserSalt(String serverName, String name, boolean isServer) throws Exception{
        String getUserSaltString;
        PreparedStatement getUserSaltPS;
        byte[] salt;
        if(doesUserExist(serverName,name) || doesServerExist(name)) {
            if (isServer == true) {
                getUserSaltString = "SELECT PasswordSalt FROM " + SERVER_TABLE + " WHERE Servername= ?";
            } else {
                getUserSaltString = "SELECT PasswordSalt FROM " + getServersUserTable(serverName) + " WHERE Username= ?";
            }

            getUserSaltPS = conn.prepareStatement(getUserSaltString);
            getUserSaltPS.setString(1, name);

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

    /*
        getServerAccessSalt: This method attempts to retrieve a specific server's server access password salt.

        @param serverName: This is the name of the server who's salt you are trying to retrieve.

        @return This method returns a byte array of the server access password salt. If the server specified does
        not exist a null byte array will be returned.
     */
    public byte[] getServerAccessSalt(String serverName) throws Exception{
        String getServerAccessSaltString = "SELECT ServerAccessPasswordSalt FROM " + SERVER_TABLE + " WHERE Servername= ?";
        PreparedStatement getServerAccessSaltPS = conn.prepareStatement(getServerAccessSaltString);
        ResultSet rst;
        byte[] salt = null;

        if(doesServerExist(serverName)){
            getServerAccessSaltPS.setString(1,serverName);
            rst = getServerAccessSaltPS.executeQuery();
            rst.next();
            salt = rst.getBytes("ServerAccessPasswordSalt");
            getServerAccessSaltPS.clearParameters();
            rst.close();
        }

        return salt;
    }

    /*
        getUserPrivilege: This method attempts to retrieve a users privilege level.

        @param serverName: The server the user belongs to.
        @param user: The name of the user who's privilege you are looking to retrieve.

        @return This method returns an integer that represents a users privilege. If the user does not exist
        it will return a -1.
     */
    public int getUserPrivilege(String serverName, String user) throws Exception{

        String getUserPrivilegeString;
        PreparedStatement getUserPrivilegePS;
        int privilege;
        ResultSet rst;

        if(doesUserExist(serverName,user)){
            getUserPrivilegeString = "SELECT Privilege FROM " + getServersUserTable(serverName) + " WHERE Username= ?";
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

    /*
        setUserPrivilege: This methods changes the privilege of a specific user on a server.

        @param serverName: The name of the server the user belongs to.
        @param user: The name of the user who's privilege you wish to change.
        @param privilege: The privilege number you would like to change the previous one to.

        @return this method returns true if the privilege is changed successfully
     */
    public boolean setUserPrivilege(String serverName, String user, int privilege) throws Exception{

        String setUserPrivilegeString;
        PreparedStatement setUserPrivilegePS;
        boolean success = false;
        if(doesUserExist(serverName,user)){
           setUserPrivilegeString = "UPDATE " + getServersUserTable(serverName) + " SET Privilege= ? WHERE Username= ?";
           setUserPrivilegePS = conn.prepareStatement(setUserPrivilegeString);
           setUserPrivilegePS.setInt(1,privilege);
           setUserPrivilegePS.setString(2,user);

           setUserPrivilegePS.executeUpdate();
           setUserPrivilegePS.clearParameters();
           success = true;
        }
        return success;
    }

    /*
        verifyPasswordHash: This method is used for verifying both server and user passwords.

        @param serverName: The name of the server or the server the user belongs to.
        @param userName: The name of the user who's password you wish to verify (if its a server leave null).
        @param passwordHash: The hashed password used for comparison with the one on the database.
        @param isServer: If you are verifying a servers password set this to true.

        @return This method will return true if the passwordHash is the same as the one in the database.
     */
    public boolean verifyPasswordHash(String serverName,String username, byte[] passwordHash, boolean isServer) throws Exception{
        String verifyPasswordHashString;
        PreparedStatement verifyPasswordHashPS;
        ResultSet rst;
        boolean response = false;

        if(doesUserExist(serverName,username)||doesServerExist(username)) {
            if (isServer == true) {
                verifyPasswordHashString = "SELECT PasswordHash FROM " + SERVER_TABLE + " WHERE Servername= ?";


            } else {
                verifyPasswordHashString = "SELECT PasswordHash FROM " + getServersUserTable(serverName) + " WHERE Username= ?";


            }

            verifyPasswordHashPS = conn.prepareStatement(verifyPasswordHashString);
            verifyPasswordHashPS.setString(1, username);

            rst = verifyPasswordHashPS.executeQuery();
            rst.next();
            response = Arrays.equals(rst.getBytes("PasswordHash"), passwordHash);

            rst.close();
            verifyPasswordHashPS.clearParameters();
        }


        return response;
    }

    /*
        verifyServerAccessPassword: This method is used for verifying a server's access password.

        @param serverName: The name of the server who's password you wish to verify.
        @param serverAccessPasswordHash: The hash of the server access password used for comparison.

        @return This method returns true if the serverAccessPasswordHash and the one on the server match.
     */
    public boolean verifyServerAccessPassword(String serverName, byte[] serverAccessPasswordHash) throws Exception{

        String verifyServerAccessPasswordHashString = "SELECT ServerAccessPasswordHash FROM " + SERVER_TABLE + " WHERE Servername= ?";
        PreparedStatement verifyServerAccessPasswordHashPS = conn.prepareStatement(verifyServerAccessPasswordHashString);
        ResultSet rst;
        boolean response = false;

        if(doesServerExist(serverName)){

            verifyServerAccessPasswordHashPS.setString(1,serverName);
            rst = verifyServerAccessPasswordHashPS.executeQuery();
            rst.next();
            response = Arrays.equals(rst.getBytes("ServerAccessPasswordHash"),serverAccessPasswordHash);
            rst.close();
            verifyServerAccessPasswordHashPS.clearParameters();
        }

        return response;


    }

    //getServerList: This method retrieves a list of all servers that are currently in the database.
    public ArrayList<String> getServerList(){
        ArrayList<String> serverNameList = new ArrayList<>();

        try {
            String getServerListString = "SELECT Servername FROM " + SERVER_TABLE;
            PreparedStatement getServerListPS = conn.prepareStatement(getServerListString);
            ResultSet rst = getServerListPS.executeQuery();
            while (rst.next()) {
                serverNameList.add(rst.getString("Servername"));
            }
            return serverNameList;
        }
        catch (Exception e){
            System.out.println(e);
        }
        return serverNameList;
    }

    public ArrayList<String> getUserList(String serverName){
        ArrayList<String> usernameList = new ArrayList<>();
        try{
            String getUserListString = "SELECT Username FROM " + getServersUserTable(serverName);
            PreparedStatement getUserListPS = conn.prepareStatement(getUserListString);
            ResultSet rst = getUserListPS.executeQuery();
            while(rst.next()){
                usernameList.add(rst.getString("Username"));
            }
        }
        catch (Exception e){
            System.out.println(e);
        }
        return  usernameList;
    }

    //closes the connection to the mysql server
    public void closeDatabaseConnection()throws Exception{
        conn.close();
    }
}
