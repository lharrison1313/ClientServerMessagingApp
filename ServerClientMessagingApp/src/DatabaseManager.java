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

    public DatabaseManager(String servername, byte[] passwordHash, byte[] passwordSalt, boolean newDatabase) throws Exception{
        Class.forName(JDBC_DRIVER);
        if(newDatabase){
            createNewDatabase();
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            createServerTable();
            addNewServer(servername, passwordHash,passwordSalt);
            userTableName = getServersUserTable(servername);
        }
        else{
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            addNewServer(servername, passwordHash,passwordSalt);
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
        String sqlServersTable = "CREATE TABLE SERVERS (Servername varchar(20), PasswordHash varbinary(512), PasswordSalt varbinary(32), UserTable varchar(20))";
        Statement stmt = conn.createStatement();
        stmt.executeUpdate(sqlServersTable);
        stmt.close();
    }

    private void addNewServer(String servername, byte[] passwordHash, byte[] passwordSalt) throws Exception{

        Statement stmt = conn.createStatement();
        ResultSet rst = stmt.executeQuery("SELECT COUNT(*) FROM SERVERS");
        rst.next();
        int userTableNO = rst.getInt(1);
        String userTableName = "Users" + userTableNO;

        String createUsersTableString = "CREATE TABLE " + "Users" + userTableNO + " (Username varchar(20), PasswordHash varbinary(512), PasswordSalt varbinary(32), Privilege int(100))";
        String insertNewServerString = "INSERT INTO " + SERVER_TABLE + "(Servername, PasswordHash, PasswordSalt, UserTable)" + " VALUES (?,?,?,?)";

        PreparedStatement createUserTablePS = conn.prepareStatement(createUsersTableString);
        PreparedStatement insertNewSeverPS = conn.prepareStatement(insertNewServerString);


        insertNewSeverPS.setString(1,servername);
        insertNewSeverPS.setBytes(2,passwordHash);
        insertNewSeverPS.setBytes(3,passwordSalt);
        insertNewSeverPS.setString(4,userTableName);

        insertNewSeverPS.executeUpdate();
        createUserTablePS.executeUpdate();


        insertNewSeverPS.clearParameters();
        createUserTablePS.clearParameters();
    }

    public boolean addNewUser(String username, byte[] passwordHash, byte[] passwordSalt, int privilege) throws  Exception{

        String insertNewUserString;
        PreparedStatement insertNewUserPS;

        if(doesUserExist(username)){
            return false;
        }
        else{
            insertNewUserString = "INSERT INTO " + userTableName + "(Username, PasswordHash, PasswordSalt, Privilege)" + " VALUES (?, ?, ?, ?)";
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

        Statement stmt = conn.createStatement();
        String sql = "SELECT Username FROM " + userTableName;

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

    public boolean doesServerExist(String servername) throws Exception{
        Statement stmt = conn.createStatement();
        String sql = "SELECT Servername FROM " + SERVER_TABLE;

        ResultSet rst = stmt.executeQuery(sql);

        while(rst.next()){
            if(rst.getString("Servername").equals(servername)){
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

    public void removeUser(String user) throws Exception{

        String removeUserString;
        PreparedStatement removeUserPS;

        if(doesUserExist(user)){
            removeUserString = "DELETE FROM " + userTableName + " WHERE Username= ?" ;
            removeUserPS = conn.prepareStatement(removeUserString);
            removeUserPS.setString(1,user);
            removeUserPS.executeUpdate();
            removeUserPS.clearParameters();
        }
    }

    public byte[] getUserSalt(String user, boolean isServer) throws Exception{

        String getUserSaltString;
        PreparedStatement getUserSaltPS;
        byte[] salt;

        if(doesUserExist(user)||doesServerExist(user)) {
            if (isServer == true) {
                getUserSaltString = "SELECT PasswordSalt FROM " + SERVER_TABLE + " WHERE Servername= ?";
            } else {

                getUserSaltString = "SELECT PasswordSalt FROM " + userTableName + " WHERE Username= ?";

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

    public int getUserPrivilege(String user) throws Exception{

        String getUserPrivilegeString;
        PreparedStatement getUserPrivilegePS;
        int privilege;
        ResultSet rst;

        if(doesUserExist(user)){
            getUserPrivilegeString = "SELECT Privilege FROM " + userTableName + " WHERE Username= ?";
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
           setUserPrivilegeString = "UPDATE " + userTableName + " SET Privilege= ? WHERE Username= ?";
           setUserPrivilegePS = conn.prepareStatement(setUserPrivilegeString);
           setUserPrivilegePS.setInt(1,privilege);
           setUserPrivilegePS.setString(2,user);

           setUserPrivilegePS.executeUpdate();
           setUserPrivilegePS.clearParameters();
        }
    }

    public boolean verifyPasswordHash(String username, byte[] passwordHash, boolean isServer) throws Exception{
        String verifyPasswordHashString;
        PreparedStatement verifyPasswordHashPS;
        ResultSet rst;
        boolean response = false;

        if(doesUserExist(username)||doesServerExist(username)) {
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

    public void closeDatabaseConnection()throws Exception{
        conn.close();
    }
}
