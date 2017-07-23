REM SET JAVA_HOME=D:\SDK\jdk6_x64

SET ZBUS_HOME=.
SET JAVA_OPTS=-server -Xms64m -Xmx1024m -XX:+UseParallelGC
SET MAIN_CLASS=org.zbus.mq.server.MqServer 
REM -ipOrder *>172>192
SET MAIN_OPTS=-conf zbus.xml -h 0.0.0.0 -p 15555 -verbose false -store store -track
SET LIB_OPTS=%ZBUS_HOME%/lib/*;%ZBUS_HOME%/classes;%ZBUS_HOME%/*;

IF NOT EXIST "%JAVA_HOME%" (
    SET JAVA=java
) ELSE (
    SET JAVA=%JAVA_HOME%\bin\java
)
"%JAVA%" %JAVA_OPTS% -cp %LIB_OPTS% %MAIN_CLASS% %MAIN_OPTS% 