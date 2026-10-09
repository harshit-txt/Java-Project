# Online Home Automation Control Platform

A simple web app where homeowners control their smart devices, set automation rules and
monitor their home, and admins manage users, device compatibility and system settings.

Built with **Java 17, Spring Boot, Spring Data JPA, Thymeleaf and an H2 database**.

## How to run

1. Install **JDK 17 or newer** (and Maven, or just open the folder in IntelliJ IDEA - it has Maven built in).
2. Open a terminal in this folder and run:

       mvn spring-boot:run

   (In IntelliJ: open the folder, wait for Maven to finish, then run `HomeAutomationApplication`.)
3. Open http://localhost:8080

The database is created automatically in the `data/` folder and filled with demo data on the first run.
To reset everything, stop the app and delete the `data/` folder.

## Demo accounts

| Role      | Email            | Password |
|-----------|------------------|----------|
| Admin     | admin@home.com   | admin123 |
| Homeowner | user@home.com    | user123  |

## What is inside (matches the project document)

**Admin**
- Overview: user/device counts, alerts, memory and uptime (System Monitoring)
- Users: add, edit, delete (User Management)
- Devices: approve or reject devices (Device Compatibility Management)
- Settings: platform name, max devices per user, allow self-registration (System Settings)

**Homeowner**
- Environment: temperature, humidity, security status (Monitor Home Environment)
- Devices: add devices, turn on/off, change brightness/speed/temperature (Device Control)
- Automation: create, enable/disable and delete rules (Set Automation Rules)
- Profile: update name, email, home name and password (Profile Management)

How devices work: a homeowner adds a device -> it is **Pending** -> the admin approves it ->
the homeowner can control it. Rules are checked every time the environment readings are saved.

## Project structure

    src/main/java/com/homeauto
        model/        User, Device, AutomationRule, EnvironmentStatus, SystemSetting   (database tables)
        repository/   one interface per table (Spring Data JPA)
        service/      password hashing, settings, rule checking, safe deleting
        controller/   AuthController, AdminController, HomeController, ApiController
        config/       login check (AuthInterceptor), demo data (DataLoader)
    src/main/resources
        templates/    Thymeleaf HTML pages (admin/, home/, login, register)
        static/css/   style.css
        application.properties

## Using MySQL instead of H2

1. In `pom.xml`, remove the comment around the `mysql-connector-j` dependency.
2. In `application.properties`, comment out the H2 lines and uncomment the MySQL lines.

## Hardware (optional, ESP32)

Two simple URLs are ready for an ESP32 (no login, demo only):

- Send sensor values: `http://<pc-ip>:8080/api/sensor?email=user@home.com&temperature=31.5&humidity=60`
- Read a device state (for an LED): `http://<pc-ip>:8080/api/device/1`  ->  `ON` or `OFF`
