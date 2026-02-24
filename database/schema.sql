CREATE DATABASE IF NOT EXISTS chatdb;
USE chatdb;

CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL
);

CREATE TABLE messages (
    id INT PRIMARY KEY AUTO_INCREMENT,
    sender VARCHAR(50),
    receiver VARCHAR(50),
    content TEXT,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE chat_groups (
    id INT PRIMARY KEY AUTO_INCREMENT,
    group_name VARCHAR(50)
);

CREATE TABLE group_members (
    group_name VARCHAR(50),
    username VARCHAR(50)
);
