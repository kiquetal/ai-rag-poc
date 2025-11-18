# Executed Instructions

This document outlines the steps executed to set up the Angular frontend with the Quarkus Quinoa extension.

## 1. Add Quarkus Quinoa Extension

**Command:**
```bash
./mvnw quarkus:add-extension -Dextensions="io.quarkiverse.quinoa:quarkus-quinoa"
```
**Description:**
Added the Quarkus Quinoa extension to the project's `pom.xml` to enable seamless integration of the frontend application with the Quarkus backend.

## 2. Create WebUI Directory

**Command:**
```bash
mkdir -p src/main/webui
```
**Description:**
Created the `src/main/webui` directory, which is the standard location for the frontend application when using the Quinoa extension.

## 3. Scaffold Angular Application

**Command:**
```bash
npx @angular/cli new frontend --directory . --skip-git --skip-install --style css --routing false
```
**Description:**
Scaffolded a new Angular application directly inside the `src/main/webui` directory. The `--directory .` flag ensures the application is created in the current directory, `--skip-git` prevents a nested Git repository, `--skip-install` defers node module installation, `--style css` sets CSS as the default stylesheet, and `--routing false` avoids creating a routing module.

## 4. Development Workflow

This project is configured for a streamlined development experience, where the Quarkus backend and Angular frontend run together with a single command.

### Prerequisites

1.  **Java & Maven**: Ensure you have a Java 21 JDK and Maven installed.
2.  **Node.js & npm**: Ensure you have Node.js (which includes npm) installed to manage the frontend dependencies.
3.  **Frontend Dependencies**: Navigate to the frontend directory and install the required packages.
    ```bash
    cd src/main/webui
    npm install
    cd ../../.. 
    ```
4.  **Environment Variables**: The application requires Gemini API credentials. Export the following environment variables:
    ```bash
    export GEMINI_API_KEY="your-api-key"
    export GEMINI_PROJECT_ID="your-project-id"
    ```

### Simplest Use Case: Dev Mode

The simplest way to run the entire application for development is to use Quarkus dev mode.

1.  **Start the Application**: Run the following command from the project root directory:
    ```bash
    ./mvnw quarkus:dev
    ```
2.  **How it Works**:
    *   This single command starts the Quarkus backend in development mode.
    *   The Quinoa extension automatically starts the Angular development server (`ng serve`).
    *   The backend is available on `http://localhost:8082`.
    *   The frontend UI is served directly from the backend URL.
    *   API requests from the frontend are automatically proxied to the backend, avoiding CORS issues.

3.  **Access the Application**:
    Open your web browser and navigate to:
    [http://localhost:8082/caton](http://localhost:8082/caton)

### Live Reloading

This setup provides a live-reloading experience for both the backend and frontend:
*   **Backend**: Any changes to the Java source files (`src/main/java`) will trigger a hot reload of the Quarkus application.
*   **Frontend**: Any changes to the Angular source files (`src/main/webui/src`) will trigger a browser refresh with the updated content.

### Environments



*   **Development (Default)**: The `./mvnw quarkus:dev` command uses the default development profile. It connects to local services and is optimized for live reloading.

*   **Production (`prod` profile)**: The configuration includes a `prod` profile (see `application.properties`) intended for packaged deployments. This profile is typically activated during a container build and is configured to connect to production services like an external Infinispan cluster, as seen in `docker-compose.yml`. For local development, you should not need to use this profile.



## 5. Angular Environment Variables



To manage different settings for development and production builds (e.g., API endpoints), you can use Angular's environment files.



1.  **Create Environment Files**:

    *   `src/main/webui/src/environments/environment.ts`: For development settings.

        ```typescript

        export const environment = {

          production: false,

          apiBaseUrl: 'http://localhost:8082'

        };

        ```

    *   `src/main/webui/src/environments/environment.prod.ts`: For production settings.

        ```typescript

        export const environment = {

          production: true,

          apiBaseUrl: '' // Served from the same origin

        };

        ```



2.  **Configure `angular.json`**:

    Modify `src/main/webui/angular.json` to replace the environment file during production builds. Add the `fileReplacements` array to the `production` configuration:

    ```json

    "architect": {

      "build": {

        "configurations": {

          "production": {

            "fileReplacements": [

              {

                "replace": "src/environments/environment.ts",

                "with": "src/environments/environment.prod.ts"

              }

            ]

            // ... other settings

          }

        }

      }

    }

    ```



3.  **Use in Your Application**:

    Import the `environment` object in your components or services:

    ```typescript

    import { environment } from '../environments/environment';



    // ...

    console.log('API URL:', environment.apiBaseUrl);

    ```
