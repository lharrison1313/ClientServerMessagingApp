import java.sql.*;
import java.util.Arrays;
public class DatabaseManager {

    private Connection conn;
    private String userTableName;
    static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://localhost/MESSAGING_APP";
    static final String USER = "root";
    static final String PASS = null;
    static final String SERVER_TABLE = "SERVERS";

    public DatabaseManager(String servername, byte[] passwordHash, byte[] passwordSalt, byte[] serverAccessPassword, byte[] serverAccessPasswordSalt, boolean newDatabase) throws Exception{
        Class.forName(JDBC_DRIVER);
        if(newDatabase){
            createNewDatabase();
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            createServerTable();
            addNewServer(servername, passwordHash,passwordSalt, serverAccessPassword,serverAccessPasswordSalt, true);
            userTableName = getServersUserTable(servername);
        }
        else{
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            addNewServer(servername, passwordHash,passwordSalt, serverAccessPassword, serverAccessPasswordSalt,false);
            userTableName = getServersUserTable(servername);
        }


    }

    public DatabaseManager(String servername)throws Exception{
        Class.forName(JDBC_DRIVER);
        conn = DriverManager.getConnection(DB_URL, USER, PASS);
        userTableName = getServersUserTable(servername);


    }


    private void createNewDatabase() throws Exception{
        Connection firstConn = DriverManager.getConnection("jdbc:mysql://localhost/",USER,PASS);
        String sql = "CREATE DATABASE MESSAGING_APP";
        Statement stmt = firstConn.createStatement();
        stmt.executeUpdate(sql);
        stmt.close();
        firstConn.close();
    }

    private void createServerTable() throws Exception{
        String sqlServersTable = "CREATE TABLE SERVERS (Servername varchar(20), PasswordHash varbinary(512), PasswordSalt varbinary(32), UserTable varchar(20), ServerAccessPasswordHash varbinary(512), ServerAccessPasswordSalt varbinary(32))";
        Statement stmt = conn.createStatement();
        stmt.executeUpdate(sqlServersTable);
        stmt.close();
    }

    private void addNewServer(String servername, byte[] passwordHash, byte[] passwordSalt, byte[] serverAccessPasswordHash, byte[] serverAccessPasswordSalt, boolean newDatabase) throws Exception{

        Statement stmt = conn.createStatement();
        ResultSet rst = stmt.executeQuery("SELECT COUNT(*) FROM SERVERS");
        rst.next();
        int userTableNO = rst.getInt(1);
        String userTableName = "Users" + userTableNO;


        String createUsersTableString = "CREATE TABLE " + "Users" + userTableNO + " (Username varchar(20), PasswordHash varbinary(512), PasswordSalt varbinary(32), Privilege int(100))";
        String insertNewServerString = "INSERT INTO " + SERVER_TABLE + "(Servername, PasswordHash, PasswordSalt, UserTable, ServerAccessPasswordHash, ServerAccessPasswordSalt)" + " VALUES (?,?,?,?,?,?)";

        PreparedStatement createUserTablePS = conn.prepareStatement(createUsersTableString);
        PreparedStatement insertNewSeverPS = conn.prepareStatement(insertNewServerString);


        insertNewSeverPS.setString(1,servername);
        insertNewSeverPS.setBytes(2,passwordHash);
        insertNewSeverPS.setBytes(3,passwordSalt);
        insertNewSeverPS.setString(4,userTableName);
        insertNewSeverPS.setBytes(5,serverAccessPasswordHash);
        insertNewSeverPS.setBytes(6,serverAccessPasswordSalt);

        insertNewSeverPS.executeUpdate();
        if(!newDatabase) {
            createUserTablePS.executeUpdate();
        }

        insertNewSeverPS.clearParameters();
        createUserTablePS.clearParameters();
    }

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

    public String getServersUserTable(String servername)throws Exception{

        if(doesServerExist(servername)) {
            String getServersUserTableString = "SELECT UserTable FROM Servers WHERE Servername=? ";
            PreparedStatement getServersUserTablePS = conn.prepareStatement(getServersUserTableString);
            getServersUserTablePS.setString(1, servername);
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

    public void removeUser(String serverName, String user) throws Exception{

        String removeUserString;
        PreparedStatement removeUserPS;

        if(doesUserExist(serverName, user)){
            removeUserString = "DELETE FROM " + getServersUserTable(serverName) + " WHERE Username= ?" ;
            removeUserPS = conn.prepareStatement(removeUserString);
            removeUserPS.setString(1,user);
            removeUserPS.executeUpdate();
            removeUserPS.clearParameters();
        }
    }

    public byte[] getUserSalt(String serverName, String user, boolean isServer) throws Exception{
        System.out.println("ye");
        String getUserSaltString;
        PreparedStatement getUserSaltPS;
        byte[] salt;
        if(doesUserExist(serverName,user) || doesServerExist(user)) {
            if (isServer == true) {
                getUserSaltString = "SELECT PasswordSalt FROM " + SERVER_TABLE + " WHERE Servername= ?";
            } else {
                getUserSaltString = "SELECT PasswordSalt FROM " + getServersUserTable(serverName) + " WHERE Username= ?";
            }

            getUserSaltPS = conn.prepareStatement(getUserSaltString);
            getUserSaltPS.setString(1, user);

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

    public void setUserPrivilege(String serverName, String user, int privilege) throws Exception{

        String setUserPrivilegeString;
        PreparedStatement setUserPrivilegePS;

        if(doesUserExist(serverName,user)){
           setUserPrivilegeString = "UPDATE " + getServersUserTable(serverName) + " SET Privilege= ? WHERE Username= ?";
           setUserPrivilegePS = conn.prepareStatement(setUserPrivilegeString);
           setUserPrivilegePS.setInt(1,privilege);
           setUserPrivilegePS.setString(2,user);

           setUserPrivilegePS.executeUpdate();
           setUserPrivilegePS.clearParameters();
        }
    }

    public boolean verifyPasswordHash(String serverName,String username, byte[] passwordHash, boolean isServer) throws Exception{
        String verifyPasswordHashString;
        PreparedStatement verifyPasswordHashPS;
        ResultSet rst;
        boolean response = false;

        if(doesUserExist(serverName,username)||doesServerExist(username)) {
            if (isServer == true) {
                verifyPasswordHashString = "SELECT PasswordHash FROM " + SERVER_TABLE + " WHERE Servername= ?";


            } else {
                verifyPasswordHashString = "SELECT PasswordHash FROM " + userTableName + " WHERE Username= ?";


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

    public void closeDatabaseConnection()throws Exception{
        conn.close();
    }
}
