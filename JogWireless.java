/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
// package jogwireless;

import java.sql.*;
import java.io.*;
import java.text.*;
import java.util.Arrays;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author yuchencai
 */
public class JogWireless {

    private static Connection connection;
    private static Statement s;
    private static Scanner input = new Scanner(System.in);
    private static ResultSet result;
    private static StringBuilder ErrorOutput = new StringBuilder();


    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        int n = service();
        while(n!=4){
            if(n==1) order();
            if(n==2) restock();
            if(n==3) streamInput();
            n=service();
        }
    }

    /**
     * read in file with certain format or write error message
     */
    public static void streamInput() {
        System.out.println("\nHere starts reading file");
        login();
        System.out.println("Please enter the name of your input file (ex. streamInput.txt):");
        String fileName = input.next();
        //streamInput.txt
        ///Users/yuchencai/NetBeansProjects/JogWireless/
        String filePath = "";
        File inFile = null;
        Scanner scan = null;
        try {
            while (true) {
                inFile = new File(filePath + fileName);
                if (inFile.canRead()) {
                    break;
                } else {
                    System.out.println("The file doens't exist please try again:");
                    fileName = input.next();
                }
            }
            scan = new Scanner(inFile);
        } catch (FileNotFoundException ex) {
            System.out.println("Error: Couldn't open file: " + filePath);
            return;
        } catch (Exception e) {
            System.out.println("Somethign is Wrong! Try Again");
        }

        try {// read every line 
            while (scan.hasNextLine()) {
                String line = scan.nextLine();
                if (line.length() > 0) {
                    processLine(line);
                }
            }
            System.out.println("\n\t Read in Record File complete!\n");
        } catch (Exception ex) {
            System.out.println("Reading File Error!");
        } finally {
            if (scan != null) {
                scan.close();
            }
        }
        PrintWriter write = null;
        String outFileName;
        try {//write to file
            System.out.println("Enter the file name for outputting error message(ex.errorOut.txt):");
            outFileName = input.next();
            File outFile = null;
            while (true) {
                outFile = new File(filePath + outFileName);
                if (outFile.canWrite()) {
                    break;
                } else {
                    System.out.println("The file doens't exist and cannot be write on, please try again:");
                    outFileName = input.next();
                }
            }
            write = new PrintWriter(outFile);
            //System.out.println("to file:" + ErrorOutput.toString());
            write.print(ErrorOutput.toString());
            System.out.println("All results are in "+outFileName+" now.");
            System.out.println("Do you want to see the results in console(y/n)?");
            boolean seeResult = tfchoice();
            if(seeResult) System.out.println(ErrorOutput.toString());
            else System.out.println("You select no.");
        } catch (FileNotFoundException ex) {
            //Logger.getLogger(JogWireless.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("File cannot be found");
        } catch(Exception ex){
            System.out.println("Unexpected execution.");
        } finally {
            close();
            if (write != null) {
                write.close();
            }
        }
    }

    /**
     * for manager to manipulate inventory and request restock
     */
    public static void restock() {
        System.out.println("Hi! Manager, begin to check inventory and restocking.");
        boolean flag = true;
        login();
        try {// selecting store ID
            System.out.println("\nManager, Please enter your current store ID (401-430)");
            String store = checkInput();
            do { // searching for matching ID
                result = exeQuery("select id, address from retail_store where id = '" + store + "'");
                if (!result.next()) {
                    System.out.println("No matches. Entered ID not in range. Try again.");
                    store = checkInput();
                } else {
                    flag = false;
                }
            } while (flag);
            flag = true; // select manufacture 
            String address = result.getString("address");
            System.out.println("\n\tYou are in store " + store + " at address " + address);
            System.out.println("Please select the manufacture of the phone (ex. Apple): ");
            String man = checkInput();
            do {// searching for result 
                result = exeQuery("select manufacture, model, id from model where manufacture like '%" + man + "%'");
                if (!result.next()) {
                    System.out.println("No matches for manufacture. Try again.");
                    man = checkInput();
                } else {
                    flag = false;
                }
            } while (flag);
            flag = true;// giving list of models 
            System.out.println("Here is a list of all matching models");
            System.out.println("Manufacture    model   id");
            System.out.println("---------------------------");
            do { // prints out the ID
                System.out.printf("%-15s%-8s%-4s\n", result.getString("manufacture"), result.getString("model"), result.getString("id"));
            } while (result.next());
            System.out.println("\nPlease enter the ID of the phone:(601-650)");
            String model = checkInput();
            do { // searching for matching phone 
                result = exeQuery("select manufacture, model, id from model where id = '" + model + "'");
                if (!result.next()) {
                    System.out.println("No matches for the id you entered. Try again.");
                    model = checkInput();
                } else {
                    flag = false;
                }
            } while (flag);
            man = result.getString("manufacture");
            System.out.println("\n\tYou have selected " + man + " model " + result.getString("model"));
            System.out.println("\nChecking inventory...\n");
            result = exeQuery("select id,quantity from inventory where s_id = '" + store + "' and m_id='" + model + "'");// s.executeQuery(query);
            boolean add;
            String inven_id;
            if (!result.next()) { // if entry doesn't exist 
                System.out.println("Current quantity of the model you selected is 0.");
                System.out.println("Do you want to reorder(y/n)?");
                add = tfchoice();
                int in;
                if (add) { // then add to it. 
                    System.out.println("Please enter the quantity you want to order from our online store:");
                    in = getInt();
                    exeUpdate("INSERT INTO inventory VALUES (seq_inventory.nextval," + store + "," + model + "," + in + ")");
                    System.out.println("\n****************************************************************");
                    System.out.println("Now "+man+" Model "+model +" has updated to: "+in);
                    System.out.println("****************************************************************\n");
                } else {
                    System.out.println("You select no, exiting...\n");
                }
            // update inventory order 
            } else {
                int total = result.getInt("quantity");
                inven_id = result.getString("ID");
                System.out.println("Current quantity is:" + total);
                System.out.println("Do you want to reorder(y/n)?");
                add = tfchoice();
                int in;
                if (add) {
                    System.out.println("Please enter the quantity you want to order from our online store:");
                    in = getInt();
                    exeUpdate("update inventory set quantity='" + (in + total) + "' where id = '" + inven_id + "'");
                    System.out.println("\n****************************************************************");
                    System.out.println("Now "+man+" Model "+model +" has updated to: "+(in+total));
                    System.out.println("****************************************************************\n");
                } else {
                    System.out.println("You select no, exiting...\n");
                }
            }
        } catch (SQLException ex) {
            //Logger.getLogger(JogWireless.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Unexpected SQL error.");
        } catch(Exception ex){
            System.out.println("Unexpected Error.");
        } finally {
            close();
        }
    }

    /**
     * service for customer ordering new phone
     */
    public static void order() {
        System.out.println("\nStarts Ordering new Phones!\n");
        boolean flag = true;
        login();
        String name = "";
        String id = "";
        boolean ifKnow;
        System.out.println("Dear Customer, Do you know your account ID? (y/n):");
        ifKnow = tfchoice();
        // asking whether user ID is know by customer
        if (ifKnow) {
            System.out.println("Dear Customer, Please enter you ID (101-150):");
            id = input.next();
        } else {
            System.out.println("Dear Customer, Please enter you name:");
            name = checkInput();
        }
        try {
            String query;
            if (!ifKnow) { // if know asking for ID if not giving name search
                do {
                    result = exeQuery("select id, name from customer where name like '%" + name + "%'");//s.executeQuery(query);
                    if (!result.next()) {
                        System.out.println("No matches. Try again.");
                        name = checkInput();
                    } else {
                        flag = false;
                    }
                } while (flag);
                flag = true;
                System.out.println("Here is a list of all matching IDs with name " + name);
                do {
                    System.out.printf("%-5s%-15s\n", result.getString("id"), result.getString("name"));
                } while (result.next());
                System.out.println("Please enter you ID based on your name:");
                id = checkInput();
            }
            // if entered id
            do {
                result = exeQuery("select id, name from customer where id = '" + id + "'");//s.executeQuery(query);
                if (!result.next()) {
                    System.out.println("No matches. Try again.");
                    id = checkInput();
                } else {
                    flag = false;
                }
            } while (flag);
            flag = true;
            name = result.getString("name");
            System.out.println("\n You have logged in as " + name + " with ID: " + id);
            System.out.println("\nPlease enter your current store ID (401-430) If not sure ask Cashier: ");
            String store = checkInput();
            do { // checking whether the store ID is with in range
                result = exeQuery("select id, address from retail_store where id = '" + store + "'");// s.executeQuery(query);
                if (!result.next()) {
                    System.out.println("No matches. Entered ID not in range. Try again.");
                    store = checkInput();
                } else {
                    flag = false;
                }
            } while (flag);
            flag = true;
            String address = result.getString("address");
            System.out.println("\n\tYou are in store " + store + " at address " + address);
            System.out.println("Please select the manufacture of the phone you want (ex. Apple,Google): ");
            String man = checkInput();
            do { // searching for manufacture 
                result = exeQuery("select manufacture, model, id from model where manufacture like '%" + man + "%'");// s.executeQuery(query);
                if (!result.next()) {
                    System.out.println("No matches for manufacture. Try again.");
                    man = checkInput();
                } else {
                    flag = false;
                }
            } while (flag);
            flag = true; // giving list of match
            System.out.println("Here is a list of all matching models");
            System.out.println("Manufacture    model   id");
            System.out.println("---------------------------");
            do { // print list of phones 
                System.out.printf("%-15s%-8s%-4s\n", result.getString("manufacture"), result.getString("model"), result.getString("id"));
            } while (result.next());
            System.out.println("\nPlease enter the ID of the phone you want to order:(601-650)");
            String model = checkInput();
            do { // searching for manufacture 
                result = exeQuery("select manufacture, model, id from model where id = '" + model + "'");//) s.executeQuery(query);
                if (!result.next()) {
                    System.out.println("No matches for the id you entered. Try again.");
                    model = checkInput();
                } else {
                    flag = false;
                }
            } while (flag);
            man = result.getString("manufacture");
            System.out.println("\n\tYou have selected " + man + " model " + result.getString("model"));
            System.out.println("Excellent Choice! Checking inventory...");
            result = exeQuery("select id,quantity from inventory where s_id = '" + store + "' and m_id='" + model + "'");// s.executeQuery(query);         
            if (!result.next()) { // print massage
                System.out.println("\n****************************************************************************************");
                System.out.println("\tSorry, we have run out of the model. Please ask our manager to order it for you");
                System.out.println("****************************************************************************************\n");
                
            } else { // if inventory entry exist 
                int total = result.getInt("quantity");
                String inven_id = result.getString("ID");
                System.out.println("Please enter the quantity: ");
                int qt = getInt();
                if (qt > total) { //see if we have enough quantity 
                    System.out.println("Sorry, we don't have enough quantity of that model. Please ask our manager to order it for you!");
                } else {
                    System.out.println("\n******************************************************************");                
                    System.out.println("\tOrder Successful. You have selected "+man+" model "+model+"!");
                    System.out.println("******************************************************************\n");
                    System.out.print("We are updating our inventory...");                   
                    exeUpdate("UPDATE inventory SET quantity = '" + (total - qt) + "'WHERE ID ='" + inven_id + "'");
                }
            }
        } catch (SQLException e) {
            System.out.println("SQL not function properly, try again!");
        } catch (Exception ex) {
            System.out.println("Something Bad Happened, try again!");
        } finally {
            close();
        }
    }

    /**
     * select interface
     *
     * @return integer indicating services
     */
    public static int service() {
        System.out.println("\nWelcome to Jog Wireless! \n");
        System.out.println("Select the service you want:");
        System.out.println("1. Customer ordering a new phone");
        System.out.println("2. Manager checking inventory and reordering");
        System.out.println("3. Usage Record: read in and output file (Stream Input)");
        System.out.println("4. Exit.\n");
        System.out.print("Please enter the number from 1-4 (ex. 1):");
        int n = getInt();
        while (n < 1 || n > 4) {
            System.out.println("The number should be from 1-4:");
            n = getInt();
        }
        System.out.println("You have select " + n);
        return n;
    }

    /**
     * testing for connection passing username and password and connect
     * statement
     *
     * @param usr
     * @param psw
     * @return boolean for whether corrected connecting
     */
    public static boolean connectDriver(String usr, String psw) {
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            // testing for connection 
            connection = DriverManager.getConnection("jdbc:oracle:thin:@edgar1.cse.lehigh.edu:1521:cse241", usr, psw);
            s = connection.createStatement();
            return connection != null; // return true if connection is not null
        } catch (SQLException ex) {
            //Logger.getLogger(JogWireless.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Connecting unsuccessful! Try again.");
        } catch (ClassNotFoundException ex) {
            //Logger.getLogger(JogWireless.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Class not found, make sure jar file in right place.");
        }
        return false;
    }

    /**
     * Close all connections
     */
    public static void close() {
        try {
            connection.close();
            s.close();
            //ps.close();
        } catch (SQLException ex) {
            //Logger.getLogger(JogWireless.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("close connections not successful.");
        }

    }

    /**
     * login check psw and repromt for valid try
     */
    public static void login() {
        boolean flag = true;
        do {
            System.out.print("Please enter your username:");
            String usr = input.next();
            System.out.print("Please enter your password:");
            String psw = input.next();
            if (connectDriver(usr, psw)) {
                flag = false;
            }
        } while (flag);
    }

    /**
     * execute query like select from
     *
     * @param query
     * @return
     */
    public static ResultSet exeQuery(String query) {
        try {
            result = s.executeQuery(query);
        } catch (SQLException ex) {
            Logger.getLogger(JogWireless.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("incorrect query");
        }
        return result;
    }

    /**
     * check for single quote
     *
     * @param inStr
     * @return boolean
     */
    public static String checkInput() {
        String inStr = input.next();
        while (inStr.contains("'")) {
            System.out.println("The single quote is not allowed in a search string.");
            inStr = input.next();
        }
        return inStr;
    }

    /**
     * execute queries with updates insertion
     *
     * @param query
     * @return whether can be updated or inserted
     */
    public static boolean exeUpdate(String query) {
        try {

            s.executeUpdate(query);
            System.out.println("Successfully updated!");
            return true;
        } catch (SQLException ex) {
            Logger.getLogger(JogWireless.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Sorry, Update unsuccessful!");
        }
        return false;
    }

    /**
     * getting input as y/n
     *
     * @return
     */
    public static boolean tfchoice() {
        String choice = input.next();
        choice = choice.toLowerCase();
        while (true) {
            if (choice.equals("y")) {
                return true;
            } else if (choice.equals("n")) {
                return false;
            } else {
                System.out.println("Not a valid input, Please enter (y/n):");
                choice = input.next();
            }
        }

    }

    /**
     * for get a number
     *
     * @return
     */
    public static int getInt() {
        while (!input.hasNextInt()) {
            input.next();
            System.out.println("Not a number! Try again");
        }
        return input.nextInt();
    }
    /**
     * dispatch the record to different method
     * @param line 
     */
    public static void processLine(String line) {
        // create another scanner object to read the previous line of Strings
        //Scanner scan = new Scanner(line);
        String[] record = line.split(",");
        //for(int i=0;i<record.length;i++) {     }
        if (record[0].toLowerCase().equals("text")) {
            if (record.length != 5) {
                writeError(record, "Incorrect number of information");
                return;
            }
            readText(record);
        } else if (record[0].toLowerCase().equals("call")) {
            if (record.length != 5) {
                writeError(record, "Incorrect number of information");
                return;
            }
            readCall(record);
        } else if (record[0].toLowerCase().equals("internet")) {
            if (record.length != 4) {
                writeError(record, "Incorrect number of information");
                return;
            }
            readInternet(record);
        } else {
            writeError(record, "No type specifier.");
        }
    }
    /**
     * reading record add into text table
     * @param record 
     */
    public static void readText(String[] record) {
        // checking if phone number exist in table 
        if (!checkPhone(record, record[1]) || !checkPhone(record, record[2])) {
            return;
        }
        // checking time format in right format
        if (!checkTimeFormat(record[3])) {
            writeError(record, "Error: Time format not accepting.");
            return;
        }
        try { // if the byte size can be parsed into int 
            Integer.parseInt(record[4]);
        } catch (NumberFormatException e) {
            writeError(record, "Error: Not an Integer for byte");
            return;
        }
        // if nothing else is wrong execute the following updates 
        try {
            exeUpdate("insert into usage values(null,'" + record[1] + "','" + record[2] + "')");
            //System.out.println("select id from usage where src ='" + record[1] + "' and dst='" + record[2] + "'");
            result = exeQuery("select id from usage where src ='" + record[1] + "' and dst='" + record[2] + "'");
            // get id from usage
            String id = "";
            while (result.next()) {
                id = result.getString("id");
            }
            result = exeQuery("select id from text where send_time = to_timestamp('" + record[3] + "','YYYY-MM-DD HH24:MI:SS') and byte_size='"+record[4]+"'");
            if (result.next()) {
                writeError(record, "Error,already exist entry with the same time and byte size");
                return;
            }          
            //System.out.println("insert into text values('" + id + "',to_timestamp('" + record[3] + "','YYYY-MM-DD HH24:MI:SS'),'" + record[4] + "')");
            // calling exec procedure 
            CallableStatement storedProc = connection.prepareCall("{call TEXT_INSERT (?,?,?)}");
            storedProc.setString(1, id);
            storedProc.setString(2, record[3]);
            storedProc.setInt(3, Integer.parseInt(record[4]));
            storedProc.execute();
            storedProc.close();
            writeError(record,"Successful Entry!");
            System.out.println("Updating TEXT table... done");
        } catch (SQLDataException ex) {
            writeError(record, "Error:data not accepted, not valid time.");
        } catch (SQLSyntaxErrorException e) {
            //Logger.getLogger(JogWireless.class.getName()).log(Level.SEVERE, null, e);
            writeError(record, "Error: wrong syntax mismatch input. ");
        } catch (SQLException ex) {
            writeError(record, "not accepting");
            //Logger.getLogger(JogWireless.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NumberFormatException e) {
            //Logger.getLogger(JogWireless.class.getName()).log(Level.SEVERE, null, e);
            writeError(record, "number format wrong");
        } catch(Exception e){
            writeError(record,"expected execution");
        }

    }
    /**
     * read line of record and insert in to call
     * @param record 
     */
    public static void readCall(String[] record) {
        try {// check phoen numebr 
            if (!checkPhone(record, record[1]) || !checkPhone(record, record[2])) {
                return;
            } // checking time format 
            if (!checkTimeFormat(record[3]) || !checkTimeFormat(record[4])) {
                writeError(record, "Error: Time format not accepting.");
                return;
            } // execute updates 
            exeUpdate("insert into usage values(null,'" + record[1] + "','" + record[2] + "')");
            //System.out.println("select id from usage where src ='" + record[1] + "' and dst='" + record[2] + "'");
            result = exeQuery("select id from usage where src ='" + record[1] + "' and dst='" + record[2] + "'");
            String id = "";
            while (result.next()) {
                id = result.getString("id");
            } // doing insertion  send_time = to_timestamp('" + record[3] + "','YYYY-MM-DD HH24:MI:SS') and byte_size='"+record[4]+"'");
            
            result = exeQuery("select id from call where start_time =to_timestamp('" + record[3] + "','YYYY-MM-DD HH24:MI:SS') and end_time=to_timestamp('" + record[4] + "','YYYY-MM-DD HH24:MI:SS')");
            if (result.next()) {
                writeError(record, "Error,already exist entry with the same start/end time");
                return;
            }
            //System.out.println("insert into text values('" + id + "','" + record[3] + "','" + record[4] + "')");
            CallableStatement storedProc = connection.prepareCall("{call CALL_INSERT (?,?,?)}");
            storedProc.setString(1, id);
            storedProc.setString(2, record[3]);
            storedProc.setString(3, record[4]);
            storedProc.execute();
            storedProc.close(); // calling stored procedure
            System.out.println("Update database... done");
            //boolean text = exeUpdate("insert into call values('" + id + "',to_timestamp('" + record[3] + "','YYYY-MM-DD HH24:MI:SS'),'" + record[4] + "')");
            writeError(record,"Successful Entry!");
        } catch (SQLDataException ex) {
            writeError(record, "Error:data not accepted, not valid time.");
        } catch (SQLException ex) {
            //Logger.getLogger(JogWireless.class.getName()).log(Level.SEVERE, null, ex);
            writeError(record,"unexpected SQL execution");
        } catch(Exception ex){
            writeError(record,"unexpected execution");
        }

    }
    /**
     * read line of record and insert in to internet
     * @param record 
     */
    public static void readInternet(String[] record) {
        try {
            if (!checkPhone(record, record[1])) {
                return;
            }
            if ((!record[2].toLowerCase().equals("upload")) && (!record[2].toLowerCase().equals("download"))) {
                writeError(record, "Error: internet acess type not recognized.");
                return;
            }
            try {
                Integer.parseInt(record[3]);
            } catch (NumberFormatException e) {
                writeError(record, "Error: Not an Integer for byte");
                return;
            }
            boolean usage = exeUpdate("insert into usage values(null,'" + record[1] + "',null)");
            //System.out.println("insert into usage values(null,'" + record[1] + "',null)");
            result = exeQuery("select id from usage where src ='" + record[1] + "' and dst is null");
            //System.out.println("after selection from");
            String id = "";
            if (result.next()) {
                id = result.getString("id");
            }
            //System.out.println("insert into internet values('" + id + "','" + record[2] + "','" + record[3] + "')");
            result = exeQuery("select id from internet where id ='" + id + "'");
            if (result.next()) {
                writeError(record, "Error,already exist entry with the same id");
                return;
            }
            //boolean text = exeUpdate("insert into internet values('" + id + "','" + record[2] + "','" + record[3] + "')");
            CallableStatement storedProc = connection.prepareCall("{call INTERNET_INSERT (?,?,?)}");
            storedProc.setString(1, id);
            storedProc.setString(2, record[2]);
            storedProc.setInt(3, Integer.parseInt(record[3]));
            storedProc.execute();
            storedProc.close();
            writeError(record,"Successful Entry!");
            System.out.println("Update database... done");
        } catch (SQLException ex) {
            //Logger.getLogger(JogWireless.class.getName()).log(Level.SEVERE, null, ex);
            writeError(record, "Error: invalid entry.");
        } catch (NumberFormatException e1) {
            //Logger.getLogger(JogWireless.class.getName()).log(Level.SEVERE, null, e1);
            writeError(record, "Error: number format wrong entry.");
        } catch (Exception ex){
            writeError(record,"Unexpected execution.");
        }
    }

    /**
     * write to string builder as error messages
     *
     * @param record
     * @param msg
     */
    public static void writeError(String[] record, String msg) {
        //System.out.println(msg);
        ErrorOutput.append(msg);
        ErrorOutput.append("  :").append(Arrays.toString(record));
        ErrorOutput.append("\n");
    }

    /**
     * checking if the phone number exist in the phone_num relation
     *
     * @param record
     * @param num
     * @return true if number exist out database
     */
    public static boolean checkPhone(String[] record, String num) {
        num = num.replaceAll("\\s", "");
        if (num.length() != 12) {
            writeError(record, "Phone number length not right");
            return false;
        }
        try {
            result = exeQuery("select * from phone_num where num='" + num + "'");
            if (result.next()) {
                return true;
            } else {
                writeError(record, "Phone Number doesn't exist.");
                return false;
            }
        } catch (SQLException ex) {
            writeError(record, "Query wrong");
            Logger.getLogger(JogWireless.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    /**
     * checking if the string can be parsed in to a particular format
     *
     * @param str
     * @return whether in correct format
     */
    public static boolean checkTimeFormat(String str) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            sdf.parse(str);
            return true;
        } catch (ParseException ex) {
            System.out.println("not a valid time:" + str);
            //ex.printStackTrace();
        }
        return false;
    }
}
