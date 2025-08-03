@echo off
echo Starting Game Server API...
echo.
echo The server will start on http://localhost:8080
echo Press Ctrl+C to stop the server
echo.
mvn compile exec:java -Dexec.mainClass="com.gameserver.api.ApiServerApplication"