@echo off
TITLE World
COLOR 02
set CLASSPATH=.;dist\*;
java -Xmx500m -Dnet.sf.odinms.listwz=false -Djavax.net.ssl.keyStore=world.keystore -Djavax.net.ssl.keyStorePassword=passwd -Djavax.net.ssl.trustStore=world.truststore -Djavax.net.ssl.trustStorePassword=passwd net.world.WorldServer
pause