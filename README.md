# SecureMessagingSystem
This project is a chat system that provides simple message encryption. The system consists of two components, the client and the server. The server is the core of the system because all connections and messages are passed through it. The client is similar to programs such as AOL instant messenger or Yahoo messenger. It provides the capabilities of both public and group chat, and instant messaging. Also, a buddy list lets you know which users are online at any given time.


# setup for databse

1: Install Mysql community Server
    -Developer default
    -development computer
    -port 3306 or if already used  change to 3307
    -Set a root password and save it
    
2: Install Mysql workbench (recommended for sql GUI)
3: Open Mysql workbench
4: open files SMSServer/database/schema.sql
5: Execute the script 
6: create database user

    CREATE USER 'chatuser'@'localhost' IDENTIFIED BY 'ChatApp123!';
GRANT ALL PRIVILEGES ON chatdb.* TO 'chatuser'@'localhost';
FLUSH PRIVILEGES;

7: make sure db.properties is in 
SMSServer/src/main/resources/ as you will need to update the local host depending on your current sql workbench port (default is 3306 but if in use, change to 3307)


db.url=jdbc:mysql://localhost:3307/chatdb
db.user=chatuser
db.password=ChatApp123!


8: run the backend in vs code through running chatserver.java and message should show 

Secure Messaging Server Started
Listening on port 5000

! leave server running for it to work! 

9: open and run testclient to use command 

SIGNUP:username:password
LOGIN:username:password
SIGNOUT
PUBLIC:message
PRIVATE:username:message
GROUP:groupName:message
ONLINE_USERS
