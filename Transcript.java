/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
//package sql;

import java.sql.*;
import java.io.*;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author yuchencai
 */
public class Transcript {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        // the first expception 
        Scanner input = null;
        try {
            input = new Scanner(System.in);
            // connection
            Class.forName("oracle.jdbc.driver.OracleDriver");
        } catch (ClassNotFoundException ex) {
            System.out.println("Error: Cannot find class! maybe the jar file ");
            //Logger.getLogger(Transcript.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            System.out.println("Something Bad Happened!");
        }
        // declare variables 
        String userID, password;
        Connection con = null;
        Statement s = null;

        try {
            System.out.print("enter Oracle user id:");
            userID = input.nextLine();
            System.out.print("enter Oracle password for " + userID + ":");
            password = input.nextLine();

            //connect to developer
            con = DriverManager.getConnection("jdbc:oracle:thin:@edgar1.cse.lehigh.edu:1521:cse241", userID, password);
        } catch (SQLException ex) {
            // Logger.getLogger(Transcript.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Counldn't find your password and user ID, try again.");
            System.out.print("enter Oracle user id:");
            userID = input.nextLine();
            System.out.print("enter Oracle password for " + userID + ":");
            password = input.nextLine();

            try {
                //connect to developer
                con = DriverManager.getConnection("jdbc:oracle:thin:@edgar1.cse.lehigh.edu:1521:cse241", userID, password);
            } catch (SQLException ex1) {
                // Logger.getLogger(Transcript.class.getName()).log(Level.SEVERE, null, ex1);
                System.out.println("Failed second chance!");
                System.out.println("Exit program now.");
                System.exit(0);

            } catch (Exception ex2) {
                System.out.println("Something Bad Happened!");
            }
        } catch (Exception ex) {
            System.out.println("Something Bad Happened!");
        }

        try {
            // put sql statement
            s = con.createStatement();
            // prompts user for what to search
            System.out.print("Input name search substring:");
            String search = input.nextLine();
            String query;
            ResultSet result = null;
            boolean flag = true;

            do {
                // test for single quote and do search
                if (search.contains("'")) {
                    System.out.println("The single quote is not allowed in a search string.");
                    search = input.nextLine();
                } else {
                    // testing query 
                    query = "select id, name from student where name like '%" + search + "%'";
                    result = s.executeQuery(query);
                    if (!result.next()) {
                        System.out.println("No matches. Try again.");
                        search = input.nextLine();
                    } else {
                        flag = false;
                    }
                }

            } while (flag);

            // print out the id with names 
            System.out.println("Here is a list of all matching IDs");
            do {
                System.out.println(result.getString("id") + "  " + result.getString("name"));
            } while (result.next());

            System.out.println("Enter the student ID for the student whose transcript you seek.");
            int student_id = 0;
            String name = "";
            do {
                // find transcript
                System.out.print("Please enter an integer between 0 to 99999:");
                if (input.hasNextInt()) {
                    student_id = input.nextInt();
                    ResultSet res = s.executeQuery("select name from student where id=" + student_id);
                    // get name for print
                    if (res.next()) {
                        name = res.getString("name");
                    }
                    query = "select year, semester, c.dept_name , c.course_id, title, "
                            + "grade from student s, takes t, course c where s.id =" + student_id
                            + " and c.COURSE_ID = t.COURSE_ID and s.ID=t.ID order by year";

                    result = s.executeQuery(query);
                } else {
                    input.next();
                }
            } while (!result.next());
                    
            // print out transcript with all the attributes it requires           
            System.out.println("\n\nTranscript for student "+student_id +" "+name);
            do {
                System.out.printf("%-5s %-7s   %-14s %4s     %-37s %-3s\n", result.getString("year"), result.getString("semester"),
                        result.getString("dept_name"), result.getString("course_id"),
                        result.getString("title"), result.getString("grade"));
            } while (result.next());

            s.close(); // have to disconnect
            con.close();

        } catch (SQLException ex) {
	    System.out.println("Something wrong about SQL, maybe query or connection.");
	    // Logger.getLogger(Transcript.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            System.out.println("Something bad happens! try re-run!");
            //Logger.getLogger(Transcript.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
