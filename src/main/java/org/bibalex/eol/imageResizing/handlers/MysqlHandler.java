package org.bibalex.eol.imageResizing.handlers;



import com.mysql.cj.jdbc.CallableStatement;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MysqlHandler {
    private org.apache.logging.log4j.Logger logger;
    private Connection conn;
    private Statement stmt;
    private CallableStatement cs;

    public MysqlHandler() {
        logger = LogHandler.getLogger(MysqlHandler.class.getName());
        conn = null;
        stmt = null;
        cs = null;
    }

    public void connectToMysql() {
        try {
            conn = (Connection) DriverManager.getConnection(ResourceHandler.getPropertyValue("mysql_url"),
                    ResourceHandler.getPropertyValue("mysql_user"),
                    ResourceHandler.getPropertyValue("mysql_password"));
            logger.info("Connection to mysql is created successfully:");


        } catch (SQLException excep) {
            excep.printStackTrace();
        } catch (Exception excep) {
            excep.printStackTrace();
        }

    }

    public void updateTableMedia(int resource_id, String base_url, String sizes) {
        try {
            cs = (CallableStatement) conn.prepareCall("{call update_sizes_in_media(?,?,?)}");
            cs.setInt(1, resource_id);
            cs.setString(2, base_url);
            cs.setString(3, sizes);
            System.out.println(cs);
            cs.execute();
//            stmt = (Statement) conn.createStatement();
//            String query = "update media set sizes = '" + sizes + "' where resource_id = " + resource_id + " and base_url = '" + base_url+"';";
//            stmt.executeUpdate(query);
//            logger.info("sizes of image with resource_id: " + resource_id + " and base_url: "+ base_url +" has been updated in the media table successfully with sizes: " + sizes);
        } catch (SQLException excep) {
            excep.printStackTrace();
        }

    }

    public void closeMysqlConnection()
    {   if (stmt != null) {
        try {
            stmt.close();
            } catch (SQLException e) { /* ignored */}
        }
        if (cs != null) {
            try {
                cs.close();
            } catch (SQLException e) { /* ignored */}
        }
        if(conn!=null){
            try {
                conn.close();
            } catch (SQLException e) { /* ignored */}
        }
        logger.info("Connection to mysql is closed successfully");
    }

}
