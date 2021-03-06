#/usr/bin
#cat zbus.sh | col -b > zbus2.sh  ==> fix win=>lin
if [ -z ${JAVA_HOME} ]; then
JAVA_HOME=/apps/jdk7
fi
ZBUS_HOME=../
JAVA_OPTS="-Dfile.encoding=UTF-8 -server -Xms64m -Xmx1024m -XX:+UseParallelGC"
MAIN_CLASS=org.zbus.broker.ha.TrackServer
MAIN_OPTS="-h 0.0.0.0 -p 16667 -verbose true"
LIB_OPTS="$ZBUS_HOME/lib/*:$ZBUS_HOME/classes:$ZBUS_HOME/*"
nohup $JAVA_HOME/bin/java $JAVA_OPTS -cp $LIB_OPTS $MAIN_CLASS $MAIN_OPTS &


