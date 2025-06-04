# DJForsenBotKotlin

## Deployment Instructions

This application requires a PostgreSQL database and consists of two services: djforsenbot and cytube-base. Follow these
instructions to deploy the application using Docker Compose.

### Prerequisites

- Docker and Docker Compose installed
- A shared Docker network named `shared_network`

### Setup

1. Create a shared Docker network if it doesn't exist already:
   ```bash
   docker network create shared_network
   ```

2. Deploy the PostgreSQL database:
   ```bash
   # Create a directory for the database stack
   mkdir -p postgres-stack
   cd postgres-stack

   # Create a docker-compose.yml file
   cat > docker-compose.yml << 'EOF'
   services:
     postgres_db:
       image: postgres:latest
       restart: always
       container_name: postgres_db
       environment:
         POSTGRES_USER: emptiness0263
         POSTGRES_PASSWORD: EN^GznyQi9L7pXWqz$
         POSTGRES_DB: your_database
       ports:
         - "5432:5432"
       volumes:
         - postgres_data:/var/lib/postgresql/data
       networks:
         - shared_network

   volumes:
     postgres_data:

   networks:
     shared_network:
       external: true
   EOF

   # Start the database
   docker-compose up -d
   ```

3. Deploy the application:
   ```bash
   # Navigate to the application directory
   cd /path/to/DJForsenBotKotlin

   # Create a docker-compose.yml file
   cat > docker-compose.yml << 'EOF'
   services:
     djforsenbot:
       image: ghcr.io/veccv/djforsenbotkotlin/djforsenbot:0.0.1
       restart: always
       ports:
        - "8080:80"
       environment:
         - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres_db:5432/your_database
         - SPRING_DATASOURCE_USERNAME=emptiness0263
         - SPRING_DATASOURCE_PASSWORD=EN^GznyQi9L7pXWqz$
         - SPOTIFY_CLIENT_ID=627d0fa26f0042e497c791047560acf7
         - SPOTIFY_CLIENT_SECRET=55839e1d7f914fe299178c2fe5546218
       networks:
         - shared_network
       depends_on:
         - cytube-base

     cytube-base:
       container_name: cytubebot-base
       image: ghcr.io/veccv/cytubebot-base/cytubebot:0.0.1
       restart: always
       ports:
         - "7777:7777"
       environment:
         - MODE=prod
       networks:
         - shared_network

   networks:
     shared_network:
       external: true
   EOF

   # Start the application
   docker-compose up -d
   ```

### Troubleshooting

If you encounter connection issues between the application and the database:

1. Make sure both Docker Compose stacks are using the same shared network.
2. Verify that the database hostname in the environment variables matches the container name in the database Docker
   Compose file.
3. Check that the database credentials in the environment variables match those in the database Docker Compose file.
4. Ensure that the database is fully started before starting the application.

### Environment Variables

The application uses the following environment variables, which are defined directly in the docker-compose.yml file:

- `SPRING_DATASOURCE_URL`: The JDBC URL for the PostgreSQL database
- `SPRING_DATASOURCE_USERNAME`: The database username
- `SPRING_DATASOURCE_PASSWORD`: The database password
- `SPOTIFY_CLIENT_ID`: The Spotify API client ID
- `SPOTIFY_CLIENT_SECRET`: The Spotify API client secret

### Database Schema Updates

The application uses Liquibase for database schema management. If you encounter the following error when starting the
application:

```
Schema-validation: missing column [is_tracking] in table [public.user]
```

This indicates that the database schema is missing columns that the application expects. This can happen if you're using
an older version of the application with a newer database schema.

A fix has been implemented in the form of a new Liquibase changelog file that adds the missing columns to the database
schema. This changelog file is included in the application and will be applied automatically when the application
starts.

If you're building the application from source, make sure to include the latest changelog files in the
`src/main/resources/db/changelog` directory.
