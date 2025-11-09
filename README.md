# ThreadDatabase

ThreadDatabase is a desktop demo application that showcases how to combine Swing UI programming, JDBC database access, and multithreading. The app manages a list of students stored in a MySQL database while animating a bouncing ball for each student on the screen to visualize concurrent work.

## Project structure

```
ThreadDatabase/
├── lib/                       # Third-party libraries (MySQL JDBC driver)
├── src/app/
│   ├── Main.java              # Application entry point (launches Swing UI)
│   ├── dao/                   # Data-access layer (JDBC queries)
│   ├── db/                    # Database utilities & configuration
│   ├── model/                 # Plain data objects shared across layers
│   ├── service/               # Async business logic (background threads)
│   └── ui/                    # Swing user interface components
└── README.md
```

### Key components

- **`app.Main`** – boots the application by delegating to Swing's event dispatch thread.
- **`app.ui.StudentApp`** – main `JFrame` containing the CRUD form, student table, and an animated `BallPanel`. It coordinates UI events and calls the service layer.
- **`app.ui.BallPanel`** – renders one bouncing ball per student using a thread per ball to demonstrate concurrency.
- **`app.service.StudentService`** – wraps a single-thread executor so JDBC work happens off the Swing thread; exposes async CRUD helpers.
- **`app.dao.StudentDAO`** – issues SQL statements against the `students` table via JDBC.
- **`app.db.DBUtil`** – loads database connection settings from `db.properties` and creates JDBC connections.
- **`app.model.Student`** – simple POJO representing a student record.

## Prerequisites

- Java 17 or later (the code uses modern language features and Swing APIs available in recent JDKs).
- MySQL server with a schema that contains a `students` table matching the DAO queries.
- The bundled MySQL JDBC driver located under `lib/mysql-connector-j-9.5.0.jar`.

### Database setup

Create the target schema and table before running the app:

```sql
CREATE DATABASE belajar CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE belajar;

CREATE TABLE students (
  id   INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  age  INT NOT NULL
);
```

Update `src/app/db/db.properties` if your MySQL host, port, database name, or credentials differ from the defaults.

## Running the application

1. Ensure the MySQL server is running and accessible with the credentials specified in `db.properties`.
2. Compile the sources from the repository root:

   ```bash
   javac -cp lib/mysql-connector-j-9.5.0.jar -d out $(find src -name "*.java")
   ```

3. Launch the Swing UI:

   ```bash
   java -cp out:lib/mysql-connector-j-9.5.0.jar app.Main
   ```

On Windows, replace `:` with `;` in the classpath.

## How the pieces work together

1. `Main` schedules `StudentApp` on the Swing event dispatch thread.
2. `StudentApp` immediately triggers `StudentService.loadAsync`, which fetches student data on a background thread and then updates the UI.
3. CRUD actions (Add, Update, Delete) use the service layer to run JDBC operations asynchronously; on completion the student list reloads.
4. `BallPanel` maintains a ball per student. When the table data refreshes, the panel syncs the balls with the database records. Each ball runs its own loop in a daemon thread, highlighting concurrent animation alongside database operations.

## Troubleshooting

- **`db.properties` not found** – ensure you run the app with the classpath including `src` (when running directly) or compiled classes so the resource is available at `/app/db/db.properties`.
- **Cannot connect to MySQL** – verify the JDBC URL, user, and password in `db.properties`, and confirm the MySQL server allows network connections.
- **Swing UI freezes** – the service layer already offloads JDBC work; if you extend the app, continue to keep blocking operations off the event dispatch thread.

## Extending the application

- Add validation by expanding `Student` with more fields and updating the DAO queries.
- Swap MySQL for another JDBC-compatible database by adjusting the driver JAR and `db.properties`.
- Replace the Swing table with JavaFX or another UI toolkit while reusing the service and DAO layers.
- Introduce tests around `StudentDAO` by using an embedded database like H2 and mocking the service layer for UI tests.

## License

This project does not currently specify a license. Add one if you intend to distribute or modify the code publicly.
