package com.grameenphone.wipro.task_executor.dao;

import com.grameenphone.wipro.task_executor.config.CbpDbConnectionPool;
import com.grameenphone.wipro.task_executor.model.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserDao {
    private static final Logger logger = LoggerFactory.getLogger(UserDao.class);

    public User findById(int id){
        Connection con = null;
        User user = null;
        try {
            con = CbpDbConnectionPool.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT name, email FROM user WHERE id = ?");
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            while(rs.next()){
                user = new User();
                user.name = rs.getString(1);
                user.emailAddress = rs.getString(2);
                break;
            }
        } catch (Exception e){
            logger.error("Error occurred while find user:: ", e);

        } finally {
            try{con.close();}catch (Exception e){}
            return user;
        }
    }

    public User findByRequestHopId(long reqHopId){
        Connection con = null;
        User user = null;
        try {
            con = CbpDbConnectionPool.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT d.name, d.email\n" +
                    "FROM payment_task a\n" +
                    "JOIN payment_request_hop b ON a.request_hop_id = b.id\n" +
                    "JOIN payment_request c ON c.id = b.request_id\n" +
                    "JOIN user d ON d.id = c.added_by_id\n" +
                    "WHERE a.request_hop_id = ?");
            pstmt.setLong(1, reqHopId);
            ResultSet rs = pstmt.executeQuery();
            while(rs.next()){
                user = new User();
                user.name = rs.getString(1);
                user.emailAddress = rs.getString(2);
                break;
            }
        } catch (Exception e){
            logger.error("Error occurred while find user:: ", e);
        } finally {
            try{con.close();}catch (Exception e){}
            return user;
        }
    }
}